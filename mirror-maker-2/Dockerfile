FROM strimzi/kafka:latest-kafka-2.4.0
USER root
RUN mkdir -p /etc/jmx_exporter
ADD ./mm2-jmx-exporter.yaml /etc/jmx_exporter
ADD ./libs/*.jar /usr/local/share/jars/
USER kafka
WORKDIR /home
COPY truststore.jks /home
COPY local-cluster/kafka-to-es.properties /home

ENV LOG_DIR=/tmp/logs
ENV EXTRA_ARGS="-javaagent:/usr/local/share/jars/jmx_prometheus_javaagent-0.12.0.jar=9400:/etc/jmx_exporter/mm2-jmx-exporter.yaml "

EXPOSE 9400

CMD /opt/kafka/bin/connect-mirror-maker.sh  /home/kafka-to-es.properties

