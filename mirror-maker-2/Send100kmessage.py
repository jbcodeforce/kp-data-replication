from confluent_kafka import Producer, KafkaError
import time 
import json, os, sys

'''
This is a basic python code to read products data and send them to kafka.
This is a good scenario for sharing reference data
'''
KAFKA_BROKERS = os.getenv('KAFKA_BROKERS')
KAFKA_APIKEY = os.getenv('KAFKA_APIKEY','')
KAFKA_CERT = os.getenv('KAFKA_CERT','')
KAFKA_USER =  os.getenv('KAFKA_USER','')
KAFKA_PWD =  os.getenv('KAFKA_PWD','')
SOURCE_TOPIC= os.getenv('KAFKA_SOURCE_TOPIC','')

options ={
    'bootstrap.servers': KAFKA_BROKERS,
    'group.id': 'ProductsProducer',
    'delivery.timeout.ms': 1900000,
    'request.timeout.ms' : 900000
}

if (KAFKA_APIKEY != '' ):
    options['security.protocol'] = 'SASL_SSL'
    options['sasl.mechanisms'] = 'PLAIN'
    options['sasl.username'] = 'token'
    options['sasl.password'] = KAFKA_APIKEY

if (KAFKA_CERT != '' ):
    options['security.protocol'] = 'SSL'
    options['ssl.ca.location'] = KAFKA_CERT

print('[KafkaProducer] - {}'.format(options))
producer=Producer(options)

def delivery_report(err, msg):
        """ Called once for each message produced to indicate delivery result.
            Triggered by poll() or flush(). """
        if err is not None:
            print('[ERROR] - [KafkaProducer] - Message delivery failed: {}'.format(err))
        else:
            print('[KafkaProducer] - Message delivered to {} [{}]'.format(msg.topic(), msg.partition()))

def publishEvent(topicName, message):
    try:
        count = 0
        for i in range(10):
            for j in range(10000):
                count += 1
                seconds = time.time()
                messageToSend = str(seconds) + " " +  message + " " + str(count)
                print(messageToSend)
                producer.produce(topicName,
                    key="p1",
                    value=messageToSend, 
                    callback=delivery_report)
    except Exception as err:
        print('Failed sending message {0}'.format(err))
        print(err)
    producer.flush()
    
def readProducts(filename):
    p = open(filename,'r')
    return json.load(p)

def signal_handler(sig,frame):
    producer.close()
    sys.exit(0)

def parseArguments():
    version="0"
    fileName="./data/products.json"
    size = 'small'
    if len(sys.argv) == 1:
        print("Usage: Send100kmessage  --file datafilename --size [small, medium, large, very-large]")
        exit(1)
    else:
        for idx in range(1, len(sys.argv)):
            arg=sys.argv[idx]
            if arg == "--size":
                size = sys.argv[idx+1]
                if size not in ['small','medium', 'large', 'very-large']:
                    print(size  + " not valid. Value can be only 'small','medium', 'large', 'very-large'")
                    print("using size small")
                    size= 'small'
            if arg == "--file":
                fileName =sys.argv[idx+1]
            if arg == "--help":
                print("Send 100k messageto a kafka cluster. Use environment variables KAFKA_BROKERS")
                print(" and KAFKA_APIKEY is the cluster accept sasl connection with token user")
                print(" and KAFKA_CERT for ca.crt to add for TLS communication")
                exit(0)
    return fileName, size

if __name__ == "__main__":
    fileName, size = parseArguments()
    products = readProducts(fileName)
    print ( fileName, size)
    message = products[size]
    try:
        publishEvent(SOURCE_TOPIC,message)
    except KeyboardInterrupt:
        producer.close()
        sys.exit(0)
    