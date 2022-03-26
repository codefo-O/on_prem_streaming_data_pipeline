<div id="top"></div>

<!-- PROJECT LOGO -->
<br />
<div align="center">
  <a href="https://github.com/codefo-O/on_prem_streaming_data_pipeline">
    <img src="images/logo.png" alt="Logo" width="300" height="300">
  </a>

<h3 align="center">ETL Pipeline</h3>

  <p align="center">
    A proof of concept project to create an streaming data pipeline to ingest data from a REST API, transform, and store in a database to visualize.
    <br />
    <a href="https://github.com/codefo-O/on_prem_streaming_data_pipeline">View Youtube Demo </a>
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

The workflow for the above diagram 

<p align="right">(<a href="#top">back to top</a>)</p>


### Built With
* [Debezium](https://debezium.io/)
* [Docker](https://www.docker.com/)
* [Kafka](https://kafka.apache.org/)
* [MySQL](https://www.mysql.com//
* [NiFi](https://nifi.apache.org/)
* [Postgres](https://www.postgresql.org/)
* [Spark](https://spark.apache.org/)
* [Superset](https://superset.apache.org/)

<p align="right">(<a href="#top">back to top</a>)</p>


<!-- GETTING STARTED -->
## Getting Started

This proof of concept streaming data pipeline will utilize NiFi to get data from a REST API.  The data will then move from a MySQL database to a PostgreSQL utalizing Kafka to produce events.  The cosumer will be setup as a simple Spark job.

### Prerequisites

This project can be ran on any server able to run Docker containers and access to the internet.

* [Docker](https://www.docker.com/)


### Deployment

To deploy the streaming_data_pipeline solution please follow the steps below.
1. Clone the repo.
   ```sh
   git clone https://github.com/codefo-O/on_prem_etl_pipeline.git
   ```
2. Change into the work directory.
   ```sh
   cd on_prem_etl_pipeline
   ```
3. Start the Postgres container.
   ```sh
   docker run -dit --name postgres \
                           -e POSTGRES_USER=airflow \
                           -e POSTGRES_PASSWORD=airflow \
                           -e POSTGRES_DB=airflow postgres:latest
   ```
4. Start the Apache Airflow & Apache Spark container.
   ```sh
   docker run -dit --name airflow \
                            -v ${PWD}/dags:/usr/local/airflow/dags \
                            -v ${PWD}/data:/data -v ${PWD}/jars:/jars \
                            -v ${PWD}/scripts:/scripts --link postgres:postgres \
                            -p 8080:8080 codefoo/airflow-spark webserver
   ```
5. Add Admin user to Apache Airflow.
   ```sh
   docker exec -it airflow airflow users create \
                                          --role Admin \
                                          --username admin \
                                          --email admin \
                                          --firstname admin \
                                          --lastname admin \
                                          --password admin
   ```
6. Update Apache Airflow database.
   ```sh
   docker exec -it airflow airflow db upgrade
   ```
7. Start Apache Airflow scheduler for the first time.
   ```sh
   docker exec -dit airflow airflow scheduler
   ```
8. Start the Apache Drill container.
   ```sh
   docker run -dit --name drill -v ${PWD}/data:/data -p 8047:8047 apache/drill:latest
   ```
9. Start the Apache Superset container.
   ```sh
   docker run -dit --name superset -p 8088:8088 codefoo/superset-sqlalchemy:latest
   ```
10. Add Admin user to Apache Superset. 
    ```sh
    docker exec -it superset superset fab create-admin \
                                           --username admin \
                                           --firstname Superset \
                                           --lastname Admin \
                                           --email admin@superset.com \
                                           --password admin
    ```
11. Update Apache Superset database.
   ```sh
   docker exec -it superset superset init
   ```
12. Start monitor for files in /data/incoming.
   ```sh
   cd data
   ./monitor_incoming.sh
   ```

<p align="right">(<a href="#top">back to top</a>)</p>


<!-- USAGE EXAMPLES -->
## Usage

Once you have completed all the steps above and started monitor incoming you can test by copying one of the test files in the data folder.
 ```sh
   cp *_Records.csv incoming/ 
 ```
The procssed files will be moved to /data/processed and failed will be moved to /data/failed

<p align="right">(<a href="#top">back to top</a>)</p>


<!-- LICENSE -->
## License

Distributed under the Apache 2.0 License. See `LICENSE.txt` for more information.

<p align="right">(<a href="#top">back to top</a>)</p>


<!-- CONTACT -->
## Contact

Gurjot Singh - GurjotSingh@rogers.com

Project Link: [https://github.com/codefo-O/on_prem_streaming_data_pipeline](https://github.com/codefo-O/on_prem_streaming_data_pipeline)