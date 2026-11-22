# syntax=docker/dockerfile:1
# Un solo Dockerfile para los 8 microservicios: se elige la carpeta con --build-arg SERVICE=ms-xxx
FROM maven:3.9-eclipse-temurin-17 AS build
ARG SERVICE
WORKDIR /app
COPY ${SERVICE}/pom.xml .
COPY ${SERVICE}/src ./src
# La cache de ~/.m2 se comparte entre los 8 builds: las dependencias se descargan una sola vez
RUN --mount=type=cache,target=/root/.m2 mvn -q -B -DskipTests package

FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]
