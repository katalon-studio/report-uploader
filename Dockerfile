FROM maven:3.9.6-eclipse-temurin-8 as build
ARG KATALON_ROOT_DIR=/katalon
RUN mkdir -p $KATALON_ROOT_DIR

WORKDIR /katalon
COPY pom.xml .
RUN mvn -B -f pom.xml -s /usr/share/maven/ref/settings-docker.xml dependency:resolve
COPY . .
RUN mvn -B -s /usr/share/maven/ref/settings-docker.xml package -DskipTests

FROM openjdk:8-jre-alpine

ENV SERVER=''
ENV EMAIL=''
ENV PASSWORD=''
ENV PROJECT_ID=''
ENV TYPE=''
ENV REPORT_PATH=''

WORKDIR /katalon
COPY --from=build /katalon/target/katalon-report-uploader-*.jar katalon-report-uploader.jar
COPY uploader.sh uploader.sh
RUN chmod a+x uploader.sh
ENV PATH "$PATH:/katalon"

WORKDIR /
COPY ./entrypoint.sh /usr/local/bin/entrypoint.sh
RUN chmod a+x /usr/local/bin/entrypoint.sh
ENTRYPOINT ["/usr/local/bin/entrypoint.sh"]
