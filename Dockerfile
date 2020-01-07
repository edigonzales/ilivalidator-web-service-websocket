FROM adoptopenjdk/openjdk11:latest

RUN apt-get update && \
    apt-get install -y curl

EXPOSE 8888

WORKDIR /home/ilivalidator

ARG DEPENDENCY=build/dependency
COPY ${DEPENDENCY}/BOOT-INF/lib /home/ilivalidator/app/lib
COPY ${DEPENDENCY}/META-INF /home/ilivalidator/app/META-INF
COPY ${DEPENDENCY}/BOOT-INF/classes /home/ilivalidator/app
RUN chown -R 1001:0 /home/ilivalidator && \
    chmod -R g=u /home/ilivalidator

USER 1001

ENTRYPOINT ["java","-cp","app:app/lib/*","ch.so.agi.ilivalidator.IlivalidatorWebServiceApplication"]

HEALTHCHECK --interval=30s --timeout=5s --start-period=60s CMD curl http://localhost:8888/actuator/health
