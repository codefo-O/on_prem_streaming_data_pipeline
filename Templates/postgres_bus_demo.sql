CREATE DATABASE bus_demo;
\c bus_demo
create table bus_status (
   record_id INT NOT NULL,
   id INT NOT NULL,
   routeId INT NOT NULL,
   directionId VARCHAR(40),
   predictable BOOLEAN,
   secsSinceReport INT NOT NULL,
   kph INT NOT NULL,
   heading INT,
   lat DECIMAL(10, 8) NOT NULL,
   lon DECIMAL(10, 8) NOT NULL,
   leadingVehicleId INT,
   event_time TIMESTAMP
);