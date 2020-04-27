#!/bin/bash
logged=$(ibmcloud account list | grep FAILED)
if [ -z "$logged"]
then
    echo $logged
    exit
fi

echo "First create source topic on event streams"
accountTopic=$(ibmcloud es topics | grep accounts)
if [ -z "$accountTopic" ]  
then
    ibmcloud es topic-create --name accounts --partitions 1 
else
    echo "$accountTopic already created "
fi

echo "Start Consumer"
cd ./perf-consumer-app/
mvn liberty:run