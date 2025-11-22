# === Build stage ===
FROM eclipse-temurin:21-jdk AS build
WORKDIR /app


COPY pom.xml .
COPY mvnw .
COPY .mvn .mvn


RUN ./mvnw -q -DskipTests dependency:go-offline


COPY src src


RUN ./mvnw -q -DskipTests clean package


FROM eclipse-temurin:21-jre
WORKDIR /app


COPY --from=build /app/target/*.jar app.jar


ENV JAVA_OPTS=""
EXPOSE 8080

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/app.jar"]