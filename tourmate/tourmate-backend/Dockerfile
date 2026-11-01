FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
# Preserve completed dependencies even when a network download interrupts a build.
# -U retries artifacts that Maven previously marked as unavailable.
RUN --mount=type=cache,id=tourmate-maven,target=/root/.m2,sharing=locked \
    set -eu; \
    for attempt in 1 2 3; do \
      if mvn -B -ntp -U -DskipTests package; then exit 0; fi; \
      if [ "$attempt" -lt 3 ]; then \
        echo "Maven build failed; retrying with cached dependencies ($attempt/3)..."; \
      fi; \
    done; \
    exit 1
FROM eclipse-temurin:21-jre
WORKDIR /app
RUN useradd --system --uid 10001 tourmate
COPY --from=build /app/target/tourmate-backend-0.1.0.jar app.jar
USER tourmate
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
