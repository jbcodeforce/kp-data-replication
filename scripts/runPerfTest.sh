#!/bin/bash

if [ -z "$(echo $0  | grep scripts)" ] 
then
  cd ..
fi

logged=$(ibmcloud account list | grep FAILED)
if [ -z "$logged"]
then
    echo $logged
    exit
else
    echo ############################
    echo Logged to IBM CLOUD
    echo ############################
fi

echo "First create source topic on event streams"
accountTopic=$(ibmcloud es topics | grep accounts)
if [ -z "$accountTopic" ]  
then
    ibmcloud es topic-create --name accounts --partitions 1 
else
    echo "$accountTopic already created "
fi

echo "Verify you are connected to an OpenShift Cluster"
ocp=$(oc get pods )
if [ -z "$ocp" ]
then
    echo "You are not connected to openshift, use oc login"
    exit
fi

targetTopic=event-streams-wdc.accounts
echo "Verify the target topic is created"
if [ -z $(oc get kafkatopics |grep  $targetTopic) ]
then
    echo "$targetTopic not created, are you sure MM2 was started"
    exit 
else
    echo "$targetTopic already created "   
fi

echo "Start Consumer"
if [ -z $(docker images | grep ibmcase/perfconsumerapp) ]
then
    echo "The docker image for the consumer is not created"
    exit
fi



echo "Start Producer "
#./script/runProducer.command