# Build app container
FROM openjdk:11.0.3-jre-slim

ARG spring_boot_jar

RUN test -n "$spring_boot_jar"

COPY $spring_boot_jar app.jar

ENTRYPOINT [ "/bin/sh", "-c", "exec java -Djava.security.egd=file:/dev/./urandom -jar app.jar" ]
