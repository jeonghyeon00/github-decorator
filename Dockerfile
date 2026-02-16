# Build stage
FROM gradle:8.5.0-jdk17 AS build
WORKDIR /app

COPY build.gradle.kts settings.gradle.kts ./

# Download dependencies
RUN gradle dependencies --no-daemon

# Copy the rest of the source code
COPY src ./src

# Build the application
RUN gradle bootJar --no-daemon

# Package stage
FROM openjdk:17.0.2-jdk-slim
WORKDIR /app

# Copy the built jar from the build stage
COPY --from=build /app/build/libs/*.jar app.jar

EXPOSE 8000
ENTRYPOINT ["java", "-jar", "app.jar"]
