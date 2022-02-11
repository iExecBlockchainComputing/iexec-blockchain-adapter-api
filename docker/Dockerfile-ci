ARG HOME=/home
ARG spring_boot_jar

# Build jar
FROM gradle:6.8.3-jdk11 as gradle-builder
ARG HOME
WORKDIR $HOME
ARG spring_boot_jar
RUN test -n "$spring_boot_jar"
COPY build.gradle gradle.properties settings.gradle  ./
COPY src/ src/
RUN gradle build -i --no-daemon

# Build app container
FROM openjdk:11.0.3-jre-slim
ARG HOME
ARG spring_boot_jar
COPY --from=gradle-builder $HOME/$spring_boot_jar app.jar
ENTRYPOINT [ "/bin/sh", "-c", "exec java -Djava.security.egd=file:/dev/./urandom -jar app.jar" ]

# CI/CD version
## git clone, then cd into
## docker image build -f docker/Dockerfile .
