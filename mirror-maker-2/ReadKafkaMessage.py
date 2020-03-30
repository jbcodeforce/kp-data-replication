from confluent_kafka import Consumer, KafkaException
import sys
import os
import getopt
import json
import logging
import time
from pprint import pformat


def stats_cb(stats_json_str):
    stats_json = json.loads(stats_json_str)
    print('\nKAFKA Stats: {}\n'.format(pformat(stats_json)))


def print_usage_and_exit(program_name):
    sys.stderr.write('Usage: %s [options..] <bootstrap-brokers> <topic1> ..\n' % program_name)
    sys.exit(1)


if __name__ == '__main__':
    # optlist, argv = getopt.getopt(sys.argv[1:], 'T:')
    # if len(argv) < 3:
    #     print_usage_and_exit(sys.argv[0])

    broker = os.getenv('KAFKA_TARGET_BROKERS')
    topics = os.getenv('KAFKA_TARGET_TOPIC')

    KAFKA_TARGET_BROKERS = os.getenv('KAFKA_TARGET_BROKERS','')
    KAFKA_TARGET_APIKEY = os.getenv('KAFKA_TARGET_APIKEY', '')
    KAFKA_TARGET_CERT = os.getenv('KAFKA_TARGET_CERT','')
    KAFKA_TARGET_USER =  os.getenv('KAFKA_TARGET_USER','')
    KAFKA_TARGET_PWD =  os.getenv('KAFKA_TARGET_PWD','')

    options = {
        'bootstrap.servers': KAFKA_TARGET_BROKERS,
        'group.id': 'ProductConsumer1',
        # 'auto.offset.reset' : 'earliest'
        }

    if (KAFKA_TARGET_APIKEY != '' ):
        options['security.protocol'] = 'SASL_SSL'
        options['sasl.mechanisms'] = 'PLAIN'
        options['sasl.username'] = 'token'
        options['sasl.password'] = KAFKA_TARGET_APIKEY

    if (KAFKA_TARGET_CERT != '' ):
        options['security.protocol'] = 'SSL'
        options['ssl.ca.location'] = KAFKA_TARGET_CERT

    # Create logger for consumer (logs will be emitted when poll() is called)
    logger = logging.getLogger('consumer')
    logger.setLevel(logging.DEBUG)
    handler = logging.StreamHandler()
    handler.setFormatter(logging.Formatter('%(asctime)-15s %(levelname)-8s %(message)s'))
    logger.addHandler(handler)

    # Create Consumer instance
    # Hint: try debug='fetch' to generate some log messages
    print('[KafkaConsumer] - {}'.format(options))
    c = Consumer(options)

    delta = 0
    totalMessageCount = 0
    # Read messages from Kafka, print to stdout
    try:
        # Subscribe to topics
        c.subscribe([topics])
        # while True:
        while totalMessageCount < 100000:
            if delta == 0:
                print(totalMessageCount)
            msg = c.poll()
            if msg is None:
                continue
            if msg.error():
                raise KafkaException(msg.error())
            else:
                message = msg.value()
                messages = message.split()
                # print(message)
                totalMessageCount += 1
                secondsNow = time.time()
                delta += (secondsNow - float(messages[0]))
                # print(secondsNow, messages[0] , secondsNow - float(messages[0]))
                if totalMessageCount % 10000 == 0:
                    print('Average '+ str(delta / 10000 ) + ' seconds / ' + str(10000) + ' message' )
                    delta = 0
    except KeyboardInterrupt:
        sys.stderr.write('%% Aborted by user\n')
    finally:
        # Close down consumer to commit final offsets.
        c.close()