#!/bin/sh
# Find and run the Spring Boot JAR
echo "Looking for JAR file in target directory..."
ls -la target/ || echo "target/ directory not found, listing current directory:"
ls -la

# Try to find the JAR file
JAR_FILE=$(find . -name "*.jar" ! -name "*-original.jar" ! -name "*-sources.jar" ! -name "*-javadoc.jar" 2>/dev/null | head -1)

if [ -z "$JAR_FILE" ]; then
  echo "Error: No JAR file found"
  echo "Searching in target directory specifically:"
  find target -name "*.jar" 2>/dev/null || echo "No JARs in target/"
  exit 1
fi

echo "Found JAR file: $JAR_FILE"
echo "Starting application..."
java -jar "$JAR_FILE"

