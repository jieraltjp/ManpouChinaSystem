FROM eclipse-temurin:21-jre-alpine AS builder
WORKDIR /app
COPY pom.xml .
COPY src src
RUN apk add --no-cache maven && \
    mvn package -DskipTests -q && \
    mv target/*.jar app.jar

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=builder /app/app.jar .
COPY --from=builder /app/src/main/resources/keys/ src/main/resources/keys/

EXPOSE 18080

# JVM 调优：容器资源限制感知
ENV JAVA_OPTS="-XX:+UseContainerSupport \
    -XX:MaxRAMPercentage=75.0 \
    -XX:InitialRAMPercentage=50.0 \
    -XX:+ExitOnOutOfMemoryError \
    -Djava.security.egd=file:/dev/./urandom"

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
