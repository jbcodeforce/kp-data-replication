from kafka.KafkaConsumer import KafkaConsumer
import kafka.EventBackboneConfiguration as ebc

TOPICNAME = "perfTopic"
CONSUMERGROUP = "OperatorConsumer-group-1"
if __name__ == "__main__":
    print("Consumer from the topic " + TOPICNAME)
    try:
        consumer = KafkaConsumer(kafka_brokers = ebc.getBrokerEndPoints(), 
                kafka_apikey = ebc.getEndPointAPIKey(), 
                kafka_cacert = ebc.getKafkaCertificate(),
                topic_name = TOPICNAME)
        consumer.prepare(CONSUMERGROUP)
        consumer.pollEvents()
    except expression as identifier:
        input('Press enter to continue')
        print("Thank you")