# Start with OL runtime.

FROM open-liberty
USER root
RUN ln -s /opt/ol/wlp/usr/servers /servers
USER 1001
ENTRYPOINT ["/opt/ol/wlp/bin/server", "run"]
CMD ["defaultServer"]
