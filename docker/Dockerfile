# Build app container
FROM eclipse-temurin:11.0.20_8-jre-focal

ARG jar

RUN test -n "$jar"

RUN apt-get update \
    && apt-get install -y curl \
    && rm -rf /var/lib/apt/lists/*

COPY $jar app.jar

ENTRYPOINT [ "java", "-Djava.security.egd=file:/dev/./urandom", "-jar", "app.jar" ]
