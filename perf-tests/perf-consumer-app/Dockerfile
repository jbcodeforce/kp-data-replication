# Start with OL runtime.
FROM open-liberty:20.0.0.3-full-java8-openj9
USER root
RUN ln -s /opt/ol/wlp/usr/servers /servers
USER 1001
COPY target/PerfConsumerApp.war /config/dropins

ENTRYPOINT ["/opt/ol/wlp/bin/server", "run"]
CMD ["defaultServer"]
