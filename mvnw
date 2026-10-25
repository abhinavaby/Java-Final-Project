#!/usr/bin/env bash
# ConnectAI Maven Wrapper Script
set -e

DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" >/dev/null 2>&1 && pwd)"

# 1. Use system 'mvn' if available
if command -v mvn >/dev/null 2>&1; then
    exec mvn "$@"
fi

# 2. Check for local Maven in ~/.m2 wrapper cache
M2_WRAPPER_MVN=$(find "$HOME/.m2/wrapper/dists" -name "mvn" -type f 2>/dev/null | head -n 1)
if [ -n "$M2_WRAPPER_MVN" ] && [ -x "$M2_WRAPPER_MVN" ]; then
    exec "$M2_WRAPPER_MVN" "$@"
fi

# 3. Check common Homebrew locations
if [ -x "/opt/homebrew/bin/mvn" ]; then
    exec /opt/homebrew/bin/mvn "$@"
elif [ -x "/usr/local/bin/mvn" ]; then
    exec /usr/local/bin/mvn "$@"
fi

echo "Error: Maven not found on PATH or in ~/.m2/wrapper/dists."
echo "Please install Maven via 'brew install maven' or run with Java directly:"
echo "  java -jar target/connectai-2.1.0.jar"
exit 1
