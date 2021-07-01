FROM openjdk:8-jdk-slim AS MAVEN_BUILD

RUN apt-get update && apt-get -y install curl

ARG MAVEN_VERSION=3.6.3
ARG SHA=c35a1803a6e70a126e80b2b3ae33eed961f83ed74d18fcd16909b2d44d7dada3203f1ffe726c17ef8dcca2dcaa9fca676987befeadc9b9f759967a8cb77181c0
ARG BASE_URL=https://apache.osuosl.org/maven/maven-3/${MAVEN_VERSION}/binaries

RUN mkdir -p /usr/share/maven /usr/share/maven/ref \
  && curl -fsSL -o /tmp/apache-maven.tar.gz ${BASE_URL}/apache-maven-${MAVEN_VERSION}-bin.tar.gz \
  && echo "${SHA}  /tmp/apache-maven.tar.gz" | sha512sum -c - \
  && tar -xzf /tmp/apache-maven.tar.gz -C /usr/share/maven --strip-components=1 \
  && rm -f /tmp/apache-maven.tar.gz \
  && ln -s /usr/share/maven/bin/mvn /usr/bin/mvn

#FROM maven:3-openjdk-15 AS MAVEN_BUILD

ENV BUILD_HOME /home/safnari

COPY . $BUILD_HOME/nsi-dds
##RUN groupadd -r safnari && useradd --home-dir $BUILD_HOME --no-log-init -r -g safnari safnari
##RUN chown -Rv safnari:safnari $BUILD_HOME
##USER safnari:safnari

WORKDIR $BUILD_HOME/nsi-dds
 
# package our application code
RUN mvn clean install -Dmaven.test.skip=true -Ddocker.nocache
 
# the second stage of our build will use open jdk 8 on alpine 3.9
FROM openjdk:8-jre-alpine3.9
#FROM openjdk:15-alpine

ENV DDS_BUILD_HOME /home/safnari/nsi-dds

WORKDIR /nsi-dds
 
# copy only the artifacts we need from the first stage and discard the rest
COPY --from=MAVEN_BUILD $DDS_BUILD_HOME/target/dds.jar .
COPY --from=MAVEN_BUILD $DDS_BUILD_HOME/config ./config

# expose port and set the startup command to execute the jar
EXPOSE 8401/tcp
CMD java \
    -Xmx1024m -Djava.net.preferIPv4Stack=true  \
    -Dcom.sun.xml.bind.v2.runtime.JAXBContextImpl.fastBoot=true \
    -Djava.util.logging.config.file=/nsi-dds/config/logging.properties \
    -Dbasedir=/nsi-dds \
    -jar /nsi-dds/dds.jar
