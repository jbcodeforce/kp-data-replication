FROM open-liberty:20.0.0.3-full-java8-openj9
COPY --chown=1001:0 src/main/liberty/config /config/

COPY  --chown=1001:0 target/PerfConsumerApp.war /config/apps
USER 1001
RUN configure.sh