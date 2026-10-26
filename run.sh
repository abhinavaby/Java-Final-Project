#!/usr/bin/env bash
# Quick launch script for ConnectAI
set -e

DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" >/dev/null 2>&1 && pwd)"
cd "$DIR"

./mvnw package -DskipTests

echo "Launching ConnectAI Desktop Application..."
java -jar target/connectai-2.2.1.jar "$@"
