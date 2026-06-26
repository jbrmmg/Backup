FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

RUN apk add --no-cache mysql-client && mkdir -p /app/logs

COPY target/MiddleTier-Backup-*.jar app.jar

ENV LOGGING_FILE_NAME=/app/logs/backup.log

VOLUME ["/app/logs"]

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
