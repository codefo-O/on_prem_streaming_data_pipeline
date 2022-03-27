package project.streaming

import project.wrapper.SparkSessionWrapper
import org.apache.spark.sql.execution.datasources.jdbc.JDBCOptions
import org.apache.spark.sql.{DataFrame, SaveMode, SparkSession}
import org.apache.spark.sql.types.StructType
import org.apache.spark.sql.functions._
import collection.JavaConverters._
import java.math.BigDecimal
import java.math.BigInteger
import java.util.Base64
import org.apache.spark.sql.SaveMode
import java.util.Properties
import org.postgresql.Driver
import org.apache.log4j.Logger

import org.apache.log4j.Level


case class JDBCConfig(url: String, user: String = "postgres", password: String = "GiVeUp", tablename: String = "bus_status")

case class KafkaReaderConfig(kafkaBootstrapServers: String, topics: String, startingOffsets: String = "latest")

case class StreamingJobConfig(checkpointLocation: String, kafkaReaderConfig: KafkaReaderConfig)

object StreamingJob extends App with SparkSessionWrapper {
 
  val currentDirectory = new java.io.File(".").getCanonicalPath
  val schema = spark.read.json("file:///jars/bus_status.json").schema
  val kafkaReaderConfig = KafkaReaderConfig("kafka:9092", "dbserver1.demo.bus_status")
  val jdbcConfig = JDBCConfig(url = "jdbc:postgresql://postgres:5432/bus_dem")
  val streamingType = "Job"
  new StreamingJobExecutor(spark, kafkaReaderConfig, currentDirectory + "/checkpoint/job/", jdbcConfig, schema, streamingType).execute()
}
object StreamingConsole extends App with SparkSessionWrapper {
  val currentDirectory = new java.io.File(".").getCanonicalPath
  val schema = spark.read.json("file:///jars/bus_status.json").schema
  val kafkaReaderConfig = KafkaReaderConfig("kafka:9092", "dbserver1.demo.bus_status")
  val jdbcConfig = JDBCConfig(url = "")
  val streamingType = "Console"
  new StreamingJobExecutor(spark, kafkaReaderConfig, currentDirectory + "/checkpoint/job/", jdbcConfig, schema, streamingType).execute()
}
object Streaminghudi extends App with SparkSessionWrapper {
  val currentDirectory = new java.io.File(".").getCanonicalPath
  val schema = spark.read.json("file:///jars/bus_status.json").schema
  val kafkaReaderConfig = KafkaReaderConfig("kafka:9092", "dbserver1.demo.bus_status")
  val jdbcConfig = JDBCConfig(url = "")
  val streamingType = "Console"
  new StreamingJobExecutor(spark, kafkaReaderConfig, currentDirectory + "/checkpoint/job/", jdbcConfig, schema, streamingType).execute()
}

class StreamingJobExecutor(spark: SparkSession, kafkaReaderConfig: KafkaReaderConfig, checkpointLocation: String, jdbcConfig: JDBCConfig, schema: StructType, streamingType: String ) {
  import spark.implicits._
  def execute(): Unit = {
    
    val convert = udf((data: String) => new BigDecimal(new BigInteger(Base64.getDecoder().decode(data)), 8).setScale(10))

    val transformDF = read().select(from_json($"value".cast("string"), schema).as("value"))
                            .select($"value.payload.after.*")
                            .withColumn("lat", convert(col("lat")).cast("decimal(10,8)"))
                            .withColumn("lon", convert(col("lon")).cast("decimal(10,8)"))
                            .withColumn("event_time", from_unixtime(col("event_time")/1000))
                            .drop("leadingVehicleId")


    if( streamingType == "Job" ){
      transformDF.writeStream
                 .option("checkpointLocation", "/checkpoint/job/")
                 .foreachBatch { (batchDF: DataFrame, _: Long) => {
                    batchDF.write
                          .format("jdbc")
                          .option("url", jdbcConfig.url)
                          .option("user", jdbcConfig.user)
                          .option("password", jdbcConfig.password)
                          .option("driver", "org.postgresql.Driver")
                          .option(JDBCOptions.JDBC_TABLE_NAME, jdbcConfig.tablename)
                          .option("stringtype", "unspecified")
                          .mode(SaveMode.Append)
                          .save()
                   }
                  }.start()
                   .awaitTermination()
    }
    else if ( streamingType == "Console" ){
      transformDF.writeStream.option("checkpointLocation", "/checkpoint/Console/")
                             .format("console")
                             .option("truncate", "false")
                             .start()
                             .awaitTermination() 
    }    
    
    else if ( streamingType == "Hudi" ){
        transformDF.writeStream.queryName("Write hudi data")
                  .foreachBatch{
                      (batchDF: DataFrame, _: Long) => {
                        batchDF.write.format("org.apache.hudi")
                        .option("hoodie.datasource.write.table.type", "COPY_ON_WRITE")
                        .option("hoodie.datasource.write.precombine.field", "event_time")
                        .option("hoodie.datasource.write.recordkey.field", "id")
                        .option("hoodie.datasource.write.partitionpath.field", "routeId")
                        .option("hoodie.table.name", "bus_status")
                        .option("hoodie.datasource.write.hive_style_partitioning", false)
                        .option("hoodie.upsert.shuffle.parallelism", "100")
                        .option("hoodie.insert.shuffle.parallelism", "100")
                        .mode(SaveMode.Append)
                        .save("/hudi/")
                      }
                  }.option("checkpointLocation", "/tmp/sparkHudi/checkpoint/")
                  .start()
                  .awaitTermination()
    }    

      
      }

    def read(): DataFrame = {
        spark
          .readStream
          .format("kafka")
          .option("kafka.bootstrap.servers", kafkaReaderConfig.kafkaBootstrapServers)
          .option("subscribe", kafkaReaderConfig.topics)
          .option("startingOffsets", kafkaReaderConfig.startingOffsets)
          .load()
    }
}
