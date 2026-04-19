# Multi-stage build for lilling-be

# Stage 1: Build
FROM gradle:8-jdk17 AS build
WORKDIR /app

# Copy Gradle wrapper and build files first for caching
COPY gradlew .
COPY gradle gradle
COPY build.gradle .
COPY settings.gradle .

# Give execute permission to Gradle wrapper
RUN chmod +x gradlew

# Download dependencies (cached layer)
RUN ./gradlew dependencies --no-daemon || true

# Copy source code
COPY src src

# Build the application (skip tests for faster build)
RUN ./gradlew clean build -x test --no-daemon

# Stage 2: Runtime
FROM amazoncorretto:17-alpine
WORKDIR /app

# Copy the built JAR from build stage
COPY --from=build /app/build/libs/*.jar app.jar

# Expose port
EXPOSE 8080

# Run the application
ENTRYPOINT ["java", "-jar", "app.jar"]
