import time 
import json, os, sys
from kafka.KafkaProducer import KafkaProducer
import kafka.EventBackboneConfiguration as ebc

GROUPID="ProducerPerformanceSnake"
NBRECORDS=1000

def parseArguments():
    version = "0"
    fileName = "./data/data.json"
    topic = "products"
    size = 'small'
    keyname = 'id'
    if len(sys.argv) == 1:
        print("Usage: ProducerPerformance  --file datafilename --size [small, medium, large]")
        exit(1)
    else:
        for idx in range(1, len(sys.argv)):
            arg=sys.argv[idx]
            if arg == "--size":
                size = sys.argv[idx+1]
                if size not in ['small','medium', 'large']:
                    print(size  + " not valid. Value can be only 'small','medium', 'large'")
                    print("using size small")
                    size= 'small'
            if arg == "--file":
                fileName =sys.argv[idx+1]
            if arg == "--topic":
                topic =sys.argv[idx+1]
            if arg == "--keyname":
                keyname =sys.argv[idx+1]
            if arg == "--help":
                print("Send n messages to a kafka cluster. Use environment variables KAFKA_BROKERS")
                print(" and KAFKA_APIKEY is the cluster accept sasl connection with token user")
                print(" and KAFKA_CERT to ca.crt path to add for TLS communication")
                print(" --file <filename including records to send in json format>")
                print(" --size small  | medium| large ")
                print("        small= 1000| medium= 10k| large= 100k")
                print(" --topic topicname")
                print(" --keyname the attribute name in the json file to be used as record key")
                exit(0)
    return fileName, size, topic,keyname

def readMessages(filename):
    p = open(filename,'r')
    return json.load(p)

def processRecords(nb_records, topicname, keyname,docsToSend):
    print("Producer to the topic " + topicname)
    try:
        producer = KafkaProducer(kafka_brokers = ebc.getBrokerEndPoints(), 
                kafka_apikey = ebc.getEndPointAPIKey(), 
                kafka_cacert = ebc.getKafkaCertificate(),
                topic_name = topicname)

        producer.prepare(groupID= GROUPID)
        a = nb_records / len(docsToSend)
        b =  nb_records % len(docsToSend)
        for i in range(0,int(a)):
            for doc in docsToSend:
                print("sending -> " + str(doc))
                producer.publishEvent(doc,keyname)
        for i in range(0,int(b)):
            print("sending -> " + str(docsToSend[i]))
            producer.publishEvent(docsToSend[i],keyname)
    except KeyboardInterrupt:
        input('Press enter to continue')
        print("Thank you")

def setNbOfRecords(size):
    if size == "medium":
        return 10000
    if size == "large":
        return 100000
    return 1000

if __name__ == "__main__":
    fileName, size, topic, keyname = parseArguments()
    messages = readMessages(fileName)
    NBRECORDS=setNbOfRecords(size)
    processRecords(NBRECORDS,topic,keyname, messages)