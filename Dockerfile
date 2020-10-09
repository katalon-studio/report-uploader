FROM maven:3.6.3-jdk-8 as build
ARG KATALON_ROOT_DIR=/katalon
RUN mkdir -p $KATALON_ROOT_DIR

WORKDIR /katalon
COPY pom.xml .
RUN mvn -B -f pom.xml -s /usr/share/maven/ref/settings-docker.xml dependency:resolve
COPY . .
RUN mvn -B -s /usr/share/maven/ref/settings-docker.xml package -DskipTests

FROM openjdk:8-jre-alpine

ENV TESTOPS_SERVER_URL=''
ENV TESTOPS_EMAIL=''
ENV TESTOPS_PASSWORD=''
ENV TESTOPS_PROJECT_ID=''
ENV TESTOPS_REPORT_TYPE=''
ENV TESTOPS_REPORT_PATH=''

WORKDIR /katalon
COPY --from=build /katalon/target/katalon-report-uploader-*.jar katalon-report-uploader.jar

WORKDIR /
COPY ./entrypoint.sh /usr/local/bin/entrypoint.sh
RUN chmod a+x /usr/local/bin/entrypoint.sh
ENTRYPOINT ["/usr/local/bin/entrypoint.sh"]
