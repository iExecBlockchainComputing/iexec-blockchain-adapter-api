# Build app container
FROM openjdk:11.0.15-jre-slim

ARG jar

RUN test -n "$jar"

COPY $jar app.jar

ENTRYPOINT [ "/bin/sh", "-c", "exec java -Djava.security.egd=file:/dev/./urandom -jar app.jar" ]
