#!/bin/bash

# Define variables
SERVICE_DIR="discord-service"
BUILD_DIR="$SERVICE_DIR/build/libs"
JAR_PATTERN="discord-service-*-all.jar"
JAVA_CMD="java"

# Check for Java 21
if ! $JAVA_CMD -version 2>&1 | grep -q "version \"21"; then
    echo "Warning: Java 21 is recommended. Found:"
    $JAVA_CMD -version
fi

# Find the jar file
JAR_FILE=$(find $BUILD_DIR -name "$JAR_PATTERN" | head -n 1)

if [ -z "$JAR_FILE" ]; then
    echo "Discord Service JAR not found. Building..."
    cd $SERVICE_DIR
    ./gradlew shadowJar
    cd ..
    JAR_FILE=$(find $BUILD_DIR -name "$JAR_PATTERN" | head -n 1)
fi

if [ -z "$JAR_FILE" ]; then
    echo "Error: Could not build or find the JAR file."
    exit 1
fi

echo "Starting Discord Service from $JAR_FILE..."
$JAVA_CMD -jar $JAR_FILE
