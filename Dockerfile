FROM eclipse-temurin:21-jdk AS build
WORKDIR /app

RUN apt-get update && \
    apt-get install -y maven

COPY pom.xml .
RUN mvn dependency:go-offline -B

COPY src ./src
RUN mvn clean package -DskipTests

FROM eclipse-temurin:21-jre
WORKDIR /app
RUN useradd -m -d /app -s /bin/bash -u 1000 appuser && \
    chown -R appuser:appuser /app

COPY --from=build --chown=appuser:appuser /app/target/project-backend*.jar app.jar
USER appuser
EXPOSE 8080 8090 10260
ENTRYPOINT ["java", "-jar", "app.jar"]