FROM bellsoft/liberica-openjdk-alpine-musl:17.0.3

ARG UID=1001
RUN adduser -S ilivalidator -u $UID

ENV HOME=/work
WORKDIR $HOME

COPY ./build/libs/ilivalidator-web-service-websocket-*-exec.jar ./application.jar
RUN chown $UID:0 . && \
    chmod 0775 . && \
    ls -la

USER $UID
EXPOSE 8888

ENV LOG4J_FORMAT_MSG_NO_LOOKUPS=true
CMD java -XX:+UseParallelGC -XX:MaxRAMPercentage=80.0 -jar application.jar