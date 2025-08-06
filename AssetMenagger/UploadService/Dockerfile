FROM openjdk:23-jdk-slim

WORKDIR /app

COPY target/UploadService.jar ./app.jar

CMD ["java", "-jar", "app.jar"]