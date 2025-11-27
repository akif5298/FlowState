#!/bin/sh
# Find and run the Spring Boot JAR
JAR_FILE=$(find target -name "*.jar" ! -name "*-original.jar" | head -1)
if [ -z "$JAR_FILE" ]; then
  echo "Error: No JAR file found in target directory"
  ls -la target/
  exit 1
fi
echo "Starting application with JAR: $JAR_FILE"
java -jar "$JAR_FILE"

