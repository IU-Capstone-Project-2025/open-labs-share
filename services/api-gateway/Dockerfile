# Stage 1: Build with eclipse-temurin JDK 21
FROM gradle:8-jdk21 AS build
WORKDIR /app

# Copy gradle files first for better caching
COPY gradle/ gradle/
COPY gradlew build.gradle settings.gradle ./

# Download dependencies (cached layer)
RUN gradle dependencies --no-daemon || true

# Copy source code
COPY src/ src/

# Build the application
RUN gradle clean && \
    gradle generateProto &&  \
    gradle build --no-daemon && \
    gradle bootJar

# Create custom minimal JRE with jlink for gRPC applications
RUN jlink --verbose \
          --module-path $JAVA_HOME/jmods \
          --strip-debug \
          --no-header-files \
          --no-man-pages \
          --compress=2 \
          --add-modules java.base,java.logging,java.naming,java.desktop,\
java.management,java.security.jgss,java.instrument,java.sql,\
jdk.unsupported,java.xml,jdk.crypto.ec,java.net.http,jdk.httpserver \
          --output /custom-jre

# Stage 2: Runtime with eclipse-temurin JRE (Ubuntu-based)
FROM eclipse-temurin:21-jre-jammy
WORKDIR /app

# Install essential dependencies
RUN apt-get update && \
    apt-get install -y --no-install-recommends wget ca-certificates && \
    apt-get clean && \
    rm -rf /var/lib/apt/lists/* && \
    mkdir -p /opt/app

# Create non-root user for security
RUN groupadd -r appgroup && \
    useradd -r -g appgroup appuser && \
    chown appuser:appgroup /opt/app

# Copy custom JRE and application jar
COPY --from=build /custom-jre /opt/jre
COPY --from=build /app/build/libs/*.jar /opt/app/app.jar

# Set ownership
RUN chown -R appuser:appgroup /opt/app /opt/jre

# Switch to non-root user
USER appuser

# Set environment variables optimized for containerized Java applications
ENV PATH="/opt/jre/bin:${PATH}" \
    JAVA_HOME="/opt/jre" \
    JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -XX:+UseG1GC -XX:+UseStringDeduplication -XX:+ExitOnOutOfMemoryError -Djava.security.egd=file:/dev/./urandom"

# Expose application port
EXPOSE 8080

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://127.0.0.1:8080/actuator/health || exit 1

# Set the entry point with optimized JVM flags
ENTRYPOINT ["/opt/jre/bin/java", \
            "-XX:+UseContainerSupport", \
            "-XX:MaxRAMPercentage=75.0", \
            "-XX:+UseG1GC", \
            "-XX:+UseStringDeduplication", \
            "-XX:+ExitOnOutOfMemoryError", \
            "-Djava.security.egd=file:/dev/./urandom", \
            "-jar", "/opt/app/app.jar"]
