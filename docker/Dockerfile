# Build app container
FROM eclipse-temurin:11.0.20_8-jre-focal

ARG jar

RUN test -n "$jar"

RUN apt-get update \
    && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/*

RUN useradd -m appuser

WORKDIR /app
COPY $jar app.jar

USER appuser

ENTRYPOINT [ "java", "-Djava.security.egd=file:/dev/./urandom", "-jar", "app.jar" ]
