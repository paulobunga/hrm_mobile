#!/bin/bash

# Exit on error
set -e

# Configuration
OPENCV_SDK_URL="https://github.com/opencv/opencv/releases/download/4.8.0/opencv-4.8.0-android-sdk.zip"
OPENCV_SDK_DIR="./opencv-android-sdk"
PROJECT_DIR="$(pwd)"

echo "Starting Android OpenCV build script..."

# Function to check if a command exists
check_command() {
    if ! command -v $1 &> /dev/null; then
        echo "Error: $1 is required but not installed."
        exit 1
    fi
}

# Check required tools
echo "Checking required tools..."
check_command wget
check_command unzip

# Check if gradlew exists
if [ ! -f "./gradlew" ]; then
    echo "Error: gradlew not found in the current directory"
    exit 1
fi

# Make gradlew executable
chmod +x ./gradlew

# Download OpenCV Android SDK if not exists
if [ ! -d "$OPENCV_SDK_DIR" ]; then
    echo "Downloading OpenCV Android SDK..."
    wget -q --show-progress "$OPENCV_SDK_URL" -O opencv-android-sdk.zip
    
    echo "Extracting OpenCV Android SDK..."
    unzip -q opencv-android-sdk.zip
    rm opencv-android-sdk.zip
else
    echo "OpenCV Android SDK already exists"
fi

# Update OpenCV build.gradle
OPENCV_BUILD_GRADLE="$OPENCV_SDK_DIR/sdk/build.gradle"
if [ -f "$OPENCV_BUILD_GRADLE" ]; then
    echo "Updating OpenCV build.gradle..."
    
    # Create a backup of the original file
    cp "$OPENCV_BUILD_GRADLE" "${OPENCV_BUILD_GRADLE}.backup"
    
    # Read the compileSdkVersion from the main project's build.gradle
    COMPILE_SDK_VERSION=$(grep 'compileSdkVersion' app/build.gradle | grep -o '[0-9]\+' || echo "34")
    
    # Create temporary file
    TEMP_FILE=$(mktemp)
    
    # Add required configurations at the start of the android block
    awk -v sdk="$COMPILE_SDK_VERSION" '
    /android {/ {
        print "android {"
        print "    namespace \"org.opencv\""
        print "    compileSdkVersion " sdk
        next
    }
    { print }
    ' "${OPENCV_BUILD_GRADLE}.backup" > "$TEMP_FILE"
    
    # Replace the original file
    mv "$TEMP_FILE" "$OPENCV_BUILD_GRADLE"
    echo "Updated OpenCV build.gradle with namespace and compileSdkVersion"
fi

# Update project's build.gradle
if [ -f "build.gradle" ]; then
    echo "Updating build.gradle..."
    
    # Check if OpenCV dependency is already added
    if ! grep -q "implementation project(':opencv')" build.gradle; then
        # Create temporary file for macOS compatible sed
        TEMP_FILE=$(mktemp)
        awk '/dependencies {/ { print; print "    implementation project(\":opencv\")"; next }1' build.gradle > "$TEMP_FILE"
        mv "$TEMP_FILE" build.gradle
        echo "Added OpenCV dependency to build.gradle"
    else
        echo "OpenCV dependency already exists in build.gradle"
    fi
else
    echo "Error: build.gradle not found in the current directory"
    exit 1
fi

# Update settings.gradle
if [ -f "settings.gradle" ]; then
    echo "Updating settings.gradle..."
    
    # Check if OpenCV module is already included
    if ! grep -q "':opencv'" settings.gradle; then
        echo "" >> settings.gradle
        echo "include ':opencv'" >> settings.gradle
        echo "project(':opencv').projectDir = new File('$OPENCV_SDK_DIR/sdk')" >> settings.gradle
        echo "Updated settings.gradle"
    else
        echo "OpenCV module already included in settings.gradle"
    fi
else
    echo "Error: settings.gradle not found in the current directory"
    exit 1
fi

# Create local.properties if it doesn't exist
if [ ! -f "local.properties" ]; then
    echo "Creating local.properties..."
    echo "sdk.dir=$ANDROID_HOME" > local.properties
fi

# Build the project
echo "Building the project..."
./gradlew clean
./gradlew build

echo "Build completed successfully!"
echo "OpenCV 4.8.0 has been integrated into your Android project"