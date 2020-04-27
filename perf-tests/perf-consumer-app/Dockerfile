FROM open-liberty:20.0.0.3-full-java8-openj9
COPY --chown=1001:0 src/main/liberty/config /config/

COPY  --chown=1001:0 target/PerfConsumerApp.war /config/apps
COPY --chown=1001:0 truststore.jks /home/truststore.jks
ENV TRUSTSTORE_PATH=/home/truststore.jks
ENV TRUSTSTORE_PWD=password
USER 1001
RUN configure.sh