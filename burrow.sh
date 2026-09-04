#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
JAR="$SCRIPT_DIR/burrow-gui/target/burrow-gui-1.0-SNAPSHOT.jar"

if [ ! -f "$JAR" ]; then
    echo "ERROR: JAR not found at $JAR"
    echo "Build first: ./mvnw clean package -DskipTests"
    exit 1
fi

exec java -jar "$JAR" "$@"
