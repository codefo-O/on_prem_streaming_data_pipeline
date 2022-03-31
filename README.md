<div id="top"></div>

<!-- PROJECT LOGO -->
<br />
<div align="center">
  <a href="https://github.com/codefo-O/on_prem_streaming_data_pipeline">
    <img src="images/logo.png" alt="Logo" width="300" height="300">
  </a>

<h3 align="center">Streaming Data Pipeline</h3>

  <p align="center">
    A proof of concept streaming data pipeline that will utilize NiFi to get data from a REST API.  The data will then move from a MySQL database to a PostgreSQL utalizing Kafka to produce events.  The cosumer will be setup as a simple Spark job.
    <br />
    <a href="https://youtu.be/IIwhjqQws1U">View Presentation Video </a>
  </p>
</div>

<!-- TABLE OF CONTENTS -->
<details>
  <summary>Table of Contents</summary>
  <ol>
    <li>
      <a href="#about-the-project">About The Project</a>
      <ul>
        <li><a href="#built-with">Built With</a></li>
      </ul>
    </li>
    <li>
      <a href="#getting-started">Getting Started</a>
      <ul>
        <li><a href="#prerequisites">Prerequisites</a></li>
        <li><a href="#Deployment">Deployment</a></li>
      </ul>
    </li>
    <li><a href="#usage">Usage</a></li>
    <li><a href="#license">License</a></li>
    <li><a href="#contact">Contact</a></li>
    </ol>
</details>

<!-- ABOUT THE PROJECT -->
## About The Project

<img src="images/diagram.png">

The workflow for the above diagram.

Step 1. NiFi will ingest data from the source Rest and write to MySQL.

Step 2. Debizeum Connect will allow Kafka to connect to MySQL to produce events.

Step 3. Spark will connect to Kafka to comsume events and write to Postgres

Step 4. User can access data from events real time

<p align="right">(<a href="#top">back to top</a>)</p>

### Built With
* [Bash](https://www.gnu.org/software/bash/)
* [Debezium](https://debezium.io/)
* [Docker](https://www.docker.com/)
* [Kafka](https://kafka.apache.org/)
* [MySQL](https://www.mysql.com/)
* [NiFi](https://nifi.apache.org/)
* [Postgres](https://www.postgresql.org/)
* [Scala](https://www.scala-lang.org/)
* [Spark](https://spark.apache.org/)
* [Superset](https://superset.apache.org/)

<p align="right">(<a href="#top">back to top</a>)</p>

<!-- GETTING STARTED -->
## Getting Started
### Prerequisites

This project can be ran on any server able to run Docker containersd.

* [Docker](https://www.docker.com/)

### Deployment

To deploy the streaming_data_pipeline solution please follow the steps below.
1. Clone the repo.
   ```sh
   git clone https://github.com/codefo-O/on_prem_streaming_data_pipeline
   ```
2. Change into the work directory.
   ```sh
   cd on_prem_streaming_data_pipeline
   ```
3. Start the MySQL container.
   ```sh
   docker run -dit --name mysql -p 3306:3306 \
                                -e MYSQL_ROOT_PASSWORD=password \
                                -e MYSQL_USER=mysqluser \
                                -e MYSQL_PASSWORD=password \
                                debezium/example-mysql:1.6
   ```
4. Create the database and table on MySQL.
   ```sh
   docker exec -i mysql mysql -u root -ppassword < templates/mysql_bus_demo.sql
   ```
5. Start the Postgres container.
   ```sh
   docker run -dit --name postgres -p 5432:5432 \
                                   -v ${PWD}/templates:/templates \
                                   -e POSTGRES_PASSWORD=password \
                                   postgres
   ```
6. Create the database and table on Postgres
   ```sh
   docker exec -i postgres psql -U postgres -f /templates/postgres_bus_demo.sql
   ```
7. Start the NiFi container.
   ```sh
   docker run -dit --name nifi -p 8080:8080 -p 8443:8443 --link mysql:mysql  codefoo/nifi-custom
   ```
8. Import/Configure the NiFi template can be done through the UI (See Demo Video) or by running the script.      
   ```sh
   ./scripts/nifi_upload_template.sh
   ```
9. Start Debezium/Zookeeper Debezium/Kafka & Debezium/Connect containers.
   ```sh
   docker run -dit --name zookeeper -p 2181:2181 -p 2888:2888 -p 3888:3888 debezium/zookeeper:1.6
   ```
   ```sh
   docker run -dit --name kafka -p 9092:9092 --link zookeeper:zookeeper debezium/kafka:1.6
   ```
   ```sh
   docker run -dit --name connect -p 8083:8083 \
                                  -e GROUP_ID=1 \
                                  -e CONFIG_STORAGE_TOPIC=my-connect-configs \
                                  -e OFFSET_STORAGE_TOPIC=my-connect-offsets \
                                  -e STATUS_STORAGE_TOPIC=my_connect_statuses \
                                  --link zookeeper:zookeeper \
                                  --link kafka:kafka \
                                  --link mysql:mysql \
                                  debezium/connect:1.6
   ```
10. Confirm Debezium Connect is running and enable MySql connector.
    ```sh
    curl -H "Accept:application/json" localhost:8083/
    ```
    ```sh
    curl -X POST -H "Accept:application/json" -H "Content-Type:application/json" localhost:8083/connectors/ -d '{ "name": "inventory-connector", "config": { "connector.class": "io.debezium.connector.mysql.MySqlConnector", "tasks.max": "1", "database.hostname": "mysql", "database.port": "3306", "database.user": "root", "database.password": "password", "database.server.id": "184054", "database.server.name": "dbserver1", "database.include.list": "bus_demo", "database.history.kafka.bootstrap.servers": "kafka:9092", "database.history.kafka.topic": "dbhistory.bus_demo" } }'
    ```
    ```sh
    curl -H "Accept:application/json" localhost:8083/connectors/inventory-connector/status
    ```
11. Start Spark master container.
    ```sh
    docker run -dit --name spark-master -p 8555:8080 -p 7077:7077 \
                                       -v ${PWD}/jars:/jars \
                                       -v ${PWD}/data:/data \
                                       -e INIT_DEAMON_STEP=setup_spark \
                                       --link kafka:kafka \
                                       --link postgres:postgres \
                                       bde2020/spark-master:3.2.0-hadoop3.2
    ```
12. Start Spark worker container.
    ```sh
    docker run -dit --name spark-worker -p 8081:8081 \
                                       -v ${PWD}/jars:/jars \
                                       -v ${PWD}/data:/data \
                                       -e SPARK_MASTER=spark://spark-master:7077 \
                                       --link kafka:kafka \
                                       --link postgres:postgres \
                                       bde2020/spark-master:3.2.0-hadoop3.2
    ```
13. Start the Superset container.
    ```sh
    docker run -dit --name superset -p 8088:8088 --link postgres:postgres apache/superset
    ```
14. Add Admin user to  Superset. 
    ```sh
    docker exec -it superset superset fab create-admin \
                                           --username admin \
                                           --firstname Superset \
                                           --lastname Admin \
                                           --email admin@superset.com \
                                           --password admin
    ```
15. Update Superset database.
    ```sh
    docker exec -it superset superset db upgrade
    ```
16. Initalize Superset.
    ```sh
    docker exec -it superset superset init
    ```
17. Start SBT container. 
    ```sh
    docker run -dit --name sbt -p 8080 -v ${PWD}/spark-streaming:/root hseeberger/scala-sbt:8u222_1.3.5_2.13.1
    ```

<p align="right">(<a href="#top">back to top</a>)</p>

<!-- USAGE EXAMPLES -->
## Usage

Once you have completed all the steps above you are ready to start the pipeline with the 2 steps below.

1. Clean the build enviornment.
```sh
docker exec -it sbt sbt clean
```
2. Build the application
```sh
docker exec -it sbt sbt assembly
```
3. Copy to jar folder.
```sh
cp spark-streaming/target/scala-2.12/spark-streaming-with-debezium_2.12.8-0.1.jar jars/
```
4. Start the NiFi Processors.
5. Start the Spark job.
```sh
docker exec -it spark-master ./spark/bin/spark-submit --master local --name streaming-data-app --class project.streaming.StreamingJDBC /jars/spark-streaming-with-debezium_2.12.8-0.1.jar
```
6. Data can now be visualized in Super Set

<p align="right">(<a href="#top">back to top</a>)</p>


<!-- LICENSE -->
## License

Distributed under the Apache 2.0 License. See `LICENSE.txt` for more information.

<p align="right">(<a href="#top">back to top</a>)</p>


<!-- CONTACT -->
## Contact

Gurjot Singh - GurjotSinghJheeta@gmail.com

Project Link: [https://github.com/codefo-O/on_prem_streaming_data_pipeline](https://github.com/codefo-O/on_prem_streaming_data_pipeline)

View Presentation Video: [https://youtu.be/IIwhjqQws1U](https://youtu.be/IIwhjqQws1U)
