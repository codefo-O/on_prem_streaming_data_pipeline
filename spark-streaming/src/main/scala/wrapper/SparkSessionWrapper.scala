package project.wrapper

import org.apache.spark.sql.SparkSession

trait SparkSessionWrapper {

  lazy val spark: SparkSession = SparkSession
    .builder
    .appName("Testing")
    .master("local[*]")
    .config("spark.serializer", "org.apache.spark.serializer.KryoSerializer")
    //to fix issue of port assignment on local
    //.config("spark.driver.bindAddress", "localhost")
    .getOrCreate()

}
