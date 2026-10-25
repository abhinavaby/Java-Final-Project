# Multi-stage Dockerfile for ConnectAI Java Swing Application
# Stage 1: Build the fat JAR
FROM maven:3.9.6-eclipse-temurin-21 AS builder
WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline -B
COPY src ./src
RUN mvn package -DskipTests

# Stage 2: Runtime with Xvfb + noVNC Web Display
FROM eclipse-temurin:21-jre-jammy

# Install Xvfb, Openbox window manager, x11vnc, noVNC, net-tools, and audio libs
RUN apt-get update && apt-get install -y \
    xvfb \
    openbox \
    x11vnc \
    novnc \
    websockify \
    libxext6 \
    libxrender1 \
    libxtst6 \
    libxi6 \
    alsa-utils \
    pulseaudio \
    && rm -rf /var/lib/apt/lists/*

WORKDIR /app
COPY --from=builder /app/target/connectai-2.0.0.jar /app/connectai.jar
COPY docker-entrypoint.sh /app/docker-entrypoint.sh
RUN chmod +x /app/docker-entrypoint.sh

# Expose noVNC web port (6080) and raw VNC port (5900)
EXPOSE 6080 5900

ENV DISPLAY=:99
ENV RESOLUTION=1600x900x24

ENTRYPOINT ["/app/docker-entrypoint.sh"]
