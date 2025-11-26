# --- build stage: JDK 23 ---
FROM maven:3.9-eclipse-temurin-23 AS build
WORKDIR /build
COPY pom.xml .
COPY src ./src
RUN mvn -q -DskipTests clean package

# --- run stage: JRE 23 ---
FROM eclipse-temurin:23-jre
WORKDIR /app
COPY --from=build /build/target/*.jar /app/app.jar
ENV SPRING_PROFILES_ACTIVE=prod
EXPOSE 8080
ENTRYPOINT ["java","-jar","/app/app.jar"]
