#!/bin/bash

# P2P Whiteboard - Build and Run Script for Linux/Mac

set -e

echo ""
echo "========================================"
echo "  P2P Whiteboard - Build Script"
echo "========================================"
echo ""

# Check if Maven is installed
if ! command -v mvn &> /dev/null; then
    echo "Error: Maven is not installed or not in PATH"
    echo "Please install Maven from https://maven.apache.org/"
    exit 1
fi

# Check if Java is installed
if ! command -v java &> /dev/null; then
    echo "Error: Java is not installed or not in PATH"
    echo "Please install Java 17 or higher from https://jdk.java.net/"
    exit 1
fi

# Display versions
echo "Checking environment..."
echo ""
java -version
echo ""
mvn --version
echo ""

# Parse command line arguments
ACTION="${1:-build}"

case $ACTION in
    build)
        echo "Building the project..."
        mvn clean package
        echo "Build completed successfully!"
        ;;
    run)
        echo "Running the application..."
        if [ ! -f "target/p2p-whiteboard.jar" ]; then
            echo "JAR file not found. Building project first..."
            mvn clean package
        fi
        echo "Starting P2P Whiteboard..."
        echo ""
        java -jar target/p2p-whiteboard.jar
        ;;
    clean)
        echo "Cleaning build artifacts..."
        mvn clean
        echo "Clean completed successfully!"
        ;;
    test)
        echo "Running tests..."
        mvn test
        ;;
    package)
        echo "Packaging the application..."
        mvn package -DskipTests
        echo "Packaging completed successfully!"
        echo "JAR file created: target/p2p-whiteboard.jar"
        ;;
    help)
        echo ""
        echo "Usage: ./build.sh [ACTION]"
        echo ""
        echo "Actions:"
        echo "  build      Build the project (default)"
        echo "  run        Build and run the application"
        echo "  clean      Clean build artifacts"
        echo "  test       Run unit tests"
        echo "  package    Package the application"
        echo "  help       Show this help message"
        echo ""
        echo "Examples:"
        echo "  ./build.sh            (builds the project)"
        echo "  ./build.sh run        (builds and runs)"
        echo "  ./build.sh clean      (cleans build)"
        echo ""
        ;;
    *)
        echo "Unknown action: $ACTION"
        echo "Run './build.sh help' for usage information"
        exit 1
        ;;
esac
