#!/bin/bash
set -e

# Run the merger on the junit-mock directory
MERGE_DIR="$(dirname "$0")"
PROJECT_ROOT="$(cd "$MERGE_DIR/.." && pwd)"
cd "$PROJECT_ROOT"

mvn compile exec:java -Dexec.mainClass="com.katalon.kit.report.uploader.UploaderApplication" -Dexec.args="merge-junit junit-mock"

# Print the merged result
echo -e "\n--- Merged Result ---\n"
cat junit-mock/merged-junit-report.xml 