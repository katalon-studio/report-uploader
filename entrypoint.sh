#!/bin/bash

set -x

java -jar katalon-report-uploader.jar \
  --server=${TESTOPS_SERVER_URL} \
  --projectId=${TESTOPS_PROJECT_ID} \
  --path="${TESTOPS_REPORT_PATH}" \
  --email="${TESTOPS_EMAIL}" \
  --password="${TESTOPS_PASSWORD}" \
  --type="${TESTOPS_REPORT_TYPE}"