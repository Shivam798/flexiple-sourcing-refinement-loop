# Build stage — Maven and the JDK come from the image, so the build does not depend on what
# is installed on the host. (Locally, ./mvnw does the same job without a Maven install.)
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /build

# Dependencies resolve in their own layer, so a code change does not re-download the world.
COPY pom.xml .
RUN mvn -B -q dependency:go-offline

COPY src ./src
RUN mvn -B -q clean package -DskipTests

# Runtime stage — a JRE, not a JDK, and not root.
FROM eclipse-temurin:21-jre AS runtime
WORKDIR /app

RUN groupadd --system sourcing && useradd --system --gid sourcing sourcing
COPY --from=build /build/target/*.jar app.jar
USER sourcing

EXPOSE 8080
ENV JAVA_OPTS="-XX:MaxRAMPercentage=75"
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar app.jar"]
