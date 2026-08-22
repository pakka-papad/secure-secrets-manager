# Stage 1: Build the application
FROM gradle:8.14.3-jdk17 AS build
WORKDIR /app
COPY build.gradle settings.gradle gradlew gradlew.bat ./
COPY gradle ./gradle
COPY src ./src
RUN gradle build -x test --no-daemon

# Stage 2: Create the final image
FROM eclipse-temurin:17-jre-focal
WORKDIR /app
COPY --from=build /app/build/libs/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
