#!/bin/bash

# Exit immediately if a command exits with a non-zero status
set -e

# Define variables
OPENCV_VERSION="4.8.0"  # Change this to the desired OpenCV version
OPENCV_SDK_URL="https://github.com/opencv/opencv/releases/download/${OPENCV_VERSION}/opencv-${OPENCV_VERSION}-android-sdk.zip"
OPENCV_SDK_DIR="opencv-sdk"
OPENCV_SDK_ZIP="opencv-sdk.zip"
MIN_JAVA_VERSION="11"
MIN_ANDROID_SDK_VERSION="34"
MIN_ANDROID_BUILD_TOOLS_VERSION="35.0.0"

# Function to check if a command exists
command_exists() {
    command -v "$1" >/dev/null 2>&1
}

# Function to check Java version
check_java_installation() {
    if [ -z "$JAVA_HOME" ] || ! command_exists java; then
        echo "Error: Java is not installed or JAVA_HOME is not set."
        echo "Please install Java ${MIN_JAVA_VERSION} or higher and set JAVA_HOME."
        exit 1
    fi

    # Get Java version
    JAVA_VERSION=$(java -version 2>&1 | awk -F '"' '/version/ {print $2}' | cut -d'.' -f1)
    if [ -z "$JAVA_VERSION" ] || [ "$JAVA_VERSION" -lt "$MIN_JAVA_VERSION" ]; then
        echo "Error: Java ${MIN_JAVA_VERSION} or higher is required."
        echo "Current version: ${JAVA_VERSION}"
        exit 1
    fi
    echo "Java version ${JAVA_VERSION} found at ${JAVA_HOME}"
}

# Function to check Android SDK installation
check_android_sdk() {
    if [ -z "$ANDROID_HOME" ] && [ -z "$ANDROID_SDK_ROOT" ]; then
        echo "Error: Android SDK not found. Neither ANDROID_HOME nor ANDROID_SDK_ROOT is set."
        echo "Please install Android SDK and set ANDROID_HOME or ANDROID_SDK_ROOT."
        exit 1
    fi

    # Prefer ANDROID_SDK_ROOT over ANDROID_HOME
    SDK_ROOT=${ANDROID_SDK_ROOT:-$ANDROID_HOME}
    
    # Check if SDK platforms are installed
    if [ ! -d "${SDK_ROOT}/platforms/android-${MIN_ANDROID_SDK_VERSION}" ]; then
        echo "Error: Android SDK Platform ${MIN_ANDROID_SDK_VERSION} not found."
        echo "Please install it using Android Studio or sdkmanager:"
        echo "sdkmanager \"platforms;android-${MIN_ANDROID_SDK_VERSION}\""
        exit 1
    fi

    # Check if build tools are installed
    if [ ! -d "${SDK_ROOT}/build-tools/${MIN_ANDROID_BUILD_TOOLS_VERSION}" ]; then
        echo "Error: Android Build Tools ${MIN_ANDROID_BUILD_TOOLS_VERSION} not found."
        echo "Please install it using Android Studio or sdkmanager:"
        echo "sdkmanager \"build-tools;${MIN_ANDROID_BUILD_TOOLS_VERSION}\""
        exit 1
    fi

    echo "Android SDK found at ${SDK_ROOT}"
    echo "Required SDK Platform and Build Tools are installed"
}

# Function to check NDK installation
check_android_ndk() {
    SDK_ROOT=${ANDROID_SDK_ROOT:-$ANDROID_HOME}
    
    # Find the latest NDK version installed
    if [ -d "${SDK_ROOT}/ndk" ]; then
        NDK_VERSION=$(ls -1 "${SDK_ROOT}/ndk" | sort -V | tail -1)
        if [ -n "$NDK_VERSION" ]; then
            echo "Android NDK version ${NDK_VERSION} found"
            return 0
        fi
    fi
    
    echo "Error: Android NDK not found."
    echo "Please install Android NDK using Android Studio or sdkmanager:"
    echo "sdkmanager \"ndk-bundle\""
    exit 1
}

# Function to download and extract OpenCV SDK
download_and_extract_opencv() {
    if [ ! -d "$OPENCV_SDK_DIR" ] || [ -z "$(ls -A $OPENCV_SDK_DIR)" ]; then
        if [ ! -f "$OPENCV_SDK_ZIP" ]; then
            echo "Downloading OpenCV Android SDK..."
            curl -L -o $OPENCV_SDK_ZIP $OPENCV_SDK_URL
        else
            echo "OpenCV Android SDK zip file already exists. Skipping download."
        fi
        
        echo "Extracting OpenCV Android SDK..."
        unzip -q $OPENCV_SDK_ZIP -d $OPENCV_SDK_DIR
        rm $OPENCV_SDK_ZIP
        echo "OpenCV Android SDK downloaded and extracted to $OPENCV_SDK_DIR"
    else
        echo "OpenCV Android SDK already exists in $OPENCV_SDK_DIR. Skipping download and extraction."
    fi
}

# Function to check and update Gradle properties
update_gradle_properties() {
    local GRADLE_PROPERTIES="gradle.properties"
    
    if [ ! -f "$GRADLE_PROPERTIES" ]; then
        echo "Creating gradle.properties file..."
        touch "$GRADLE_PROPERTIES"
    fi

    # Add or update Android SDK location
    if ! grep -q "sdk.dir=" "$GRADLE_PROPERTIES"; then
        echo "sdk.dir=${ANDROID_SDK_ROOT:-$ANDROID_HOME}" >> "$GRADLE_PROPERTIES"
    fi

    # Add or update Java home
    if ! grep -q "org.gradle.java.home=" "$GRADLE_PROPERTIES"; then
        echo "org.gradle.java.home=$JAVA_HOME" >> "$GRADLE_PROPERTIES"
    fi

    echo "Gradle properties updated successfully"
}

# Function to build the Android project
build_android_project() {
    # Ensure gradlew is executable
    if [ ! -x "./gradlew" ]; then
        echo "Making gradlew executable..."
        chmod +x gradlew
    fi
    
    echo "Building Android project..."
    ./gradlew build
    echo "Android project built successfully."
}

# Main script
main() {
    echo "Starting Android OpenCV setup..."
    
    # Check if necessary commands exist
    if ! command_exists curl; then
        echo "Error: curl is not installed. Please install it and try again."
        exit 1
    fi
    
    if ! command_exists unzip; then
        echo "Error: unzip is not installed. Please install it and try again."
        exit 1
    fi

    # Check all requirements
    check_java_installation
    check_android_sdk
    check_android_ndk
    
    # Update Gradle properties
    update_gradle_properties
    
    # Download and extract OpenCV SDK
    download_and_extract_opencv
    
    # Build the Android project
    build_android_project
    
    echo "All tasks completed successfully."
}

# Run the main function
main