FROM openjdk:8-jre-alpine

ENV TESTOPS_SERVER_URL=''
ENV TESTOPS_EMAIL=''
ENV TESTOPS_PASSWORD=''
ENV TESTOPS_PROJECT_ID=''
ENV TESTOPS_REPORT_TYPE=''
ENV TESTOPS_REPORT_PATH=''

COPY target/katalon-report-uploader-*.jar /katalon-report-uploader.jar
COPY entrypoint.sh /entrypoint.sh
RUN chmod a+x /entrypoint.sh /katalon-report-uploader.jar

ENTRYPOINT ["/entrypoint.sh"]
