FROM openjdk:8-jre-alpine

ENV TESTOPS_SERVER_URL=''
ENV TESTOPS_EMAIL=''
ENV TESTOPS_PASSWORD=''
ENV TESTOPS_PROJECT_ID=''
ENV TESTOPS_REPORT_TYPE=''
ENV TESTOPS_REPORT_PATH=''

ARG KATALON_ROOT_DIR=/katalon
RUN mkdir -p $KATALON_ROOT_DIR

WORKDIR $KATALON_ROOT_DIR
COPY target/katalon-report-uploader-*.jar katalon-report-uploader.jar

WORKDIR /
COPY ./entrypoint.sh /usr/local/bin/entrypoint.sh
RUN chmod a+x /usr/local/bin/entrypoint.sh
ENTRYPOINT ["/usr/local/bin/entrypoint.sh"]
