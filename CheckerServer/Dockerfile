FROM openjdk:21-jdk-slim

WORKDIR /app

COPY target/CheckerService.jar app.jar

CMD ["java", "-jar", "app.jar"]