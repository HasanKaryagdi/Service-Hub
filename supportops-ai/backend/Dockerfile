FROM maven:3.9.11-eclipse-temurin-21 AS build
WORKDIR /src
COPY pom.xml .
RUN mvn -B -ntp dependency:resolve
COPY src src
RUN mvn -B -ntp verify
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /src/target/supportops-api-0.1.0.jar app.jar
USER 10001
EXPOSE 8080
ENTRYPOINT ["java","-XX:MaxRAMPercentage=70","-jar","app.jar"]
