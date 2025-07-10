#!/bin/bash

# JUnit XML Merger Script
# Usage: ./merge-junit.sh <directory-path>
# Example: ./merge-junit.sh "Reports/20250710_193226/Traffic Agent/Verify Data Tracking On Support Elements"

if [ $# -eq 0 ]; then
    echo "Usage: $0 <directory-path>"
    echo "Example: $0 \"Reports/20250710_193226/Traffic Agent/Verify Data Tracking On Support Elements\""
    exit 1
fi

DIRECTORY_PATH="$1"

if [ ! -d "$DIRECTORY_PATH" ]; then
    echo "Error: Directory '$DIRECTORY_PATH' does not exist"
    exit 1
fi

echo "Merging JUnit XML files in directory: $DIRECTORY_PATH"

# Run the merger using Maven
mvn exec:java -Dexec.mainClass="com.katalon.kit.report.uploader.UploaderApplication" -Dexec.args="merge-junit '$DIRECTORY_PATH'" -q

if [ $? -eq 0 ]; then
    echo "JUnit XML files merged successfully!"
    echo "Merged file location: $DIRECTORY_PATH/merged-junit-report.xml"
else
    echo "Failed to merge JUnit XML files"
    exit 1
fi 