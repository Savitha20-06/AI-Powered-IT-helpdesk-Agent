FROM eclipse-temurin:25-jdk AS build
WORKDIR /app
COPY pom.xml .
RUN apt-get update && apt-get install -y maven \
    && mvn -q -DskipTests dependency:go-offline \
    && rm -rf /var/lib/apt/lists/*
COPY src src
RUN mvn -q -DskipTests package

FROM eclipse-temurin:25-jre
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java","-jar","/app/app.jar"]
