---
page_type: sample
languages:
- java
products:
- azure
description: "This sample shows how to use Spring Data Apache Cassandra module with Azure CosmosDB service."
urlFragment: spring-data-cassandra-on-azure
---

# Spring Data Apache Cassandra on Azure

This sample shows how to use Spring Data Apache Cassandra module with Azure CosmosDB service.

## TOC

- [Prerequisite](#prerequisite)
- [Build](#build)
- [Run](#run)

## Prerequisite

- Azure Account
- JDK 1.8 or above
- Maven 3.0 or above
- Curl

## Build

1. Create a Cassandra account with Azure CosmosDB by following tutorial at 
[here](https://docs.microsoft.com/en-us/azure/cosmos-db/create-cassandra-java#create-a-database-account).

1. Use [Data Explorer](https://docs.microsoft.com/en-us/azure/cosmos-db/data-explorer) from Azure Portal to create a keyspace named `mykeyspace`. 

1. Find `application.properties` at `src/main/resources` and fill in below properties.

    ```
    spring.data.cassandra.contact-points=<replace with your Cassandra contact point>
    spring.data.cassandra.port=10350
    spring.data.cassandra.username=<replace with your Cassandra account user name>
    spring.data.cassandra.password=<replace with your Cassandra account password>
    ```

1. Build the sample application into a `JAR` package by running below command.
   
   ```shell
   mvn clean package
   ```

## Run

Following below steps to run and test the sample application.

1. Run application.

    ```shell
    java -jar target/spring-data-cassandra-on-azure-0.1.0-SNAPSHOT.jar
    ```

1. Create new users by running below command.

    ```shell
    curl -s -d '{"name":"Tom","species":"cat"}' -H "Content-Type: application/json" -X POST http://localhost:8080/pets
    curl -s -d '{"name":"Jerry","species":"mouse"}' -H "Content-Type: application/json" -X POST http://localhost:8080/pets
    ```
    
    Sample output is as below.
    ```text
    Added Pet(id=1, name=Tom, species=cat).
    ...
    Added Pet(id=2, name=Jerry, species=mouse).
    ```

1. Get all existing pets by running below command.

    ```shell
    curl -s http://localhost:8080/pets
    ```
    
    Sample output is as below.
    ```txt
    [{"id":1,"name":"Tom","species":"cat"},{"id":2,"name":"Jerry","species":"mouse"}]
    ```
