FROM eclipse-temurin:21.0.12_8-jre-noble AS extractor

ARG jar

RUN test -n "$jar"

WORKDIR /extractor

COPY $jar iexec-blockchain-adapter-api.jar

RUN java -Djarmode=tools -jar iexec-blockchain-adapter-api.jar extract --layers

FROM eclipse-temurin:21.0.12_8-jre-noble

RUN apt-get update \
    && apt-get install -y --no-install-recommends curl=8.5.0-2ubuntu10.13 \
    && apt-get upgrade -y \
    && rm -rf /var/lib/apt/lists/*

RUN groupadd -g 1001 appuser \
    && useradd -g 1001 --no-create-home -s /sbin/nologin -u 1001 appuser

RUN install -d -o 1001 -g 1001 /app

COPY --from=extractor --chown=1001:1001 /extractor/iexec-blockchain-adapter-api/dependencies/ /app
COPY --from=extractor --chown=1001:1001 /extractor/iexec-blockchain-adapter-api/snapshot-dependencies/ /app
COPY --from=extractor --chown=1001:1001 /extractor/iexec-blockchain-adapter-api/application/ /app

USER 1001
WORKDIR /app
ENTRYPOINT [ "java", "-Djava.security.egd=file:/dev/./urandom", "-jar", "iexec-blockchain-adapter-api.jar" ]
