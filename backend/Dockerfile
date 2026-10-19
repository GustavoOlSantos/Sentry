
# Etapa 1: compilar o backend com Java 21 e Maven
FROM maven:3.9-eclipse-temurin-21 AS build

WORKDIR /app

COPY pom.xml mvnw ./
COPY .mvn .mvn

RUN chmod +x mvnw
RUN ./mvnw -B dependency:go-offline

COPY src src
RUN ./mvnw -B clean package -DskipTests

# Etapa 2: executar a aplicação com Java 21
FROM eclipse-temurin:21-jre

WORKDIR /app

# O Sentry utiliza o Nmap para as varreduras
RUN apt-get update \
    && apt-get install -y --no-install-recommends nmap \
    && rm -rf /var/lib/apt/lists/*

COPY --from=build /app/target/sentry-*.jar app.jar

EXPOSE 8080

ENV NVD_API_KEY=""

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
