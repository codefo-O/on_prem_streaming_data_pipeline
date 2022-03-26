create database bus_demo;
use bus_demo;
create table bus_status (
   record_id INT NOT NULL AUTO_INCREMENT,
   id INT NOT NULL,
   routeId INT NOT NULL,
   directionId VARCHAR(40),
   predictable BOOLEAN,
   secssinceReport INT NOT NULL,
   kph INT NOT NULL,
   heading INT,
   lat DECIMAL(10, 8) NOT NULL,
   lon DECIMAL(10, 8) NOT NULL,
   leadingVehicleId INT,
   event_time DATETIME DEFAULT NOW(),
   PRIMARY KEY (record_id) 
);