ARG HOME=/home
ARG JAR_PATH=$HOME/build/libs/*.jar

# Build jar
FROM gradle:6.8.3-jdk11 as gradle-builder
ARG HOME
WORKDIR $HOME
COPY build.gradle gradle.properties settings.gradle  ./
COPY src/ src/
RUN gradle build -i --no-daemon

# Build app container
FROM openjdk:11.0.3-jre-slim
ARG JAR_PATH
COPY --from=gradle-builder $JAR_PATH app.jar
ENTRYPOINT [ "/bin/sh", "-c", "exec java -Djava.security.egd=file:/dev/./urandom -jar app.jar" ]

# CI/CD version
## git clone, then cd into
## docker image build -f docker/Dockerfile .