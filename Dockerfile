# Build stage
FROM maven:3.9.6-eclipse-temurin-17-alpine AS build
WORKDIR /app
COPY pom.xml .
COPY src src
RUN mvn clean package -DskipTests

# Run stage
FROM eclipse-temurin:17-jre-alpine

# Use a non-root user
RUN addgroup -g 65532 nonroot && adduser -u 65532 -G nonroot -s /bin/sh -D nonroot
USER 65532:65532

VOLUME /tmp
COPY --from=build --chown=65532:65532 /app/target/*.jar app.jar
ENTRYPOINT ["java","-jar","app.jar"]
