#!/bin/sh -l

set -ex

addition_args=""

if [ "$TESTOPS_SERVER_URL" != "" ]; then
    addition_args="${addition_args} --server=${TESTOPS_SERVER_URL}"
fi

if [ "$TESTOPS_EMAIL" != "" ]; then
    addition_args="${addition_args} --email=${TESTOPS_EMAIL}"
fi

java -jar /katalon/katalon-report-uploader.jar --projectId=${TESTOPS_PROJECT_ID} --path="${TESTOPS_REPORT_PATH}" --password="${TESTOPS_PASSWORD}" --type="${TESTOPS_REPORT_TYPE}" ${addition_args}