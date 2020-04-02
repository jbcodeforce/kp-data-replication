#!/bin/bash
echo "##########################################################"
echo " A docker image for python 3.7 development: "
echo "##########################################################"
echo
name=python37
docker run --network="kafkanet" -e KAFKA_BROKERS=$KAFKA_BROKERS --rm --name $name -v $(pwd):/home -it  ibmcase/python37 bash 