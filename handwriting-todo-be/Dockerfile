# Stage 1: Build Spring Boot jar
FROM eclipse-temurin:21-jdk-alpine AS build
WORKDIR /workspace
COPY gradle gradle
COPY gradlew .
COPY settings.gradle .
COPY build.gradle .
COPY handwriting-todo-be handwriting-todo-be
RUN ./gradlew :handwriting-todo-be:bootJar -PskipFrontend --no-daemon

# Stage 2: Run
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /workspace/handwriting-todo-be/build/libs/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]