from kafka.KafkaConsumer import KafkaConsumer
import kafka.EventBackboneConfiguration as ebc
from confluent_kafka import KafkaException
import json, time , sys


def parseArguments():
    version = "0"
    topic = "products"
    for idx in range(1, len(sys.argv)):
        arg=sys.argv[idx]
        if arg == "--topic":
            topic =sys.argv[idx+1]
        if arg == "--help":
            print("Usage: PerfConsumer --topic topicName")
            print("read messages from a kafka cluster. Use environment variables KAFKA_BROKERS")
            print(" and KAFKA_APIKEY is the cluster accept sasl connection with token user")
            print(" and KAFKA_CERT to ca.crt path to add for TLS communication")
            print(" --topic topicname")
            exit(0)
    return topic


if __name__ == "__main__":
    TOPICNAME = parseArguments()
    CONSUMERGROUP = "PerfConsumer-group-1"
    print("Consumer from the topic " + TOPICNAME)
    try:
        consumer = KafkaConsumer(kafka_brokers = ebc.getBrokerEndPoints(), 
                kafka_apikey = ebc.getEndPointAPIKey(), 
                kafka_cacert = ebc.getKafkaCertificate(),
                autocommit = False,
                fromWhere = 'earliest',
                topic_name = TOPICNAME)
        consumer.prepare(CONSUMERGROUP)
        delta = 0
        totalMessageCount = 0
        gotIt = False
        while not gotIt:
            try :
                msg = consumer.consumer.poll()
                if msg is None:
                    continue
                if msg.error():
                    raise KafkaException(msg.error())
                else:
                    message = msg.value()
                    try:
                        message_json = json.loads(message)
                        print(message_json)
                    except Exception as e:
                        continue
                    totalMessageCount += 1
                    secondsNow = time.time()
                    delta += (secondsNow - float(message_json['timestamp']))
                    print("Now: " + str(secondsNow) + " ts:" + str(message_json['timestamp']) + " delta:" + str(secondsNow - float(message_json['timestamp'])))
                    if totalMessageCount % 100 == 0:
                        print('Average '+ str(delta / 1000 ) + ' seconds / ' + str(1000) + ' message' )
                        delta = 0
            except KeyboardInterrupt as identifier:
                print(identifier)
                input('Press enter to continue')
                print("Thank you")
    except Exception as identifier:
        print(identifier)
        input('Press enter to continue')
        print("Thank you")