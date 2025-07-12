FROM eclipse-temurin:17-jdk as builder

WORKDIR /app

# Установка зависимостей для сборки TDLib
RUN apt-get update && apt-get install -y \
    git cmake build-essential zlib1g-dev libssl-dev \
    && rm -rf /var/lib/apt/lists/*

# Копируем исходный код
COPY pom.xml .
COPY src ./src

# Сборка проекта
RUN ./mvnw clean package

# Финальный образ
FROM eclipse-temurin:17-jre

WORKDIR /app

# Копируем собранный JAR
COPY --from=builder /app/target/telegram-file-downloader-jar-with-dependencies.jar ./app.jar

# Установка нативных зависимостей для TDLib
RUN apt-get update && apt-get install -y \
    zlib1g libssl1.1 \
    && rm -rf /var/lib/apt/lists/*

# Настройка переменных окружения
ENV DOWNLOAD_DIR=/data/downloads
ENV TDLOG_VERBOSITY=1

VOLUME ["/data/downloads", "/app/tdlib_db"]

CMD ["java", "-jar", "app.jar"]