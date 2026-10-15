# ─── Build stage ──────────────────────────────────────────
FROM eclipse-temurin:21-jdk-alpine AS builder

WORKDIR /app

# Копируем Maven wrapper и pom — для кэширования зависимостей
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./

# Даём права на выполнение и скачиваем зависимости
RUN chmod +x mvnw && ./mvnw dependency:go-offline -B

# Копируем исходники и собираем
COPY src/ src/
RUN ./mvnw package -DskipTests -B

# ─── Runtime stage ────────────────────────────────────────
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# Копируем собранный jar из build-стадии
COPY --from=builder /app/target/*.jar app.jar

EXPOSE 8082

# Healthcheck через Actuator
HEALTHCHECK --interval=15s --timeout=5s --start-period=60s --retries=10 \
    CMD wget -q --spider http://localhost:8082/actuator/health || exit 1

CMD ["java", "-jar", "app.jar"]