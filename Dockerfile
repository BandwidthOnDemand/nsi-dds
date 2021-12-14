FROM maven:3-openjdk-15 AS MAVEN_BUILD

ENV BUILD_HOME /home/safnari

ENV HOME /nsi-dds
WORKDIR $HOME
COPY . .
RUN mvn clean install -Dmaven.test.skip=true -Ddocker.nocache
 
FROM openjdk:15

ENV HOME /nsi-dds
USER 1000:1000
WORKDIR $HOME
COPY --from=MAVEN_BUILD $HOME/target/dds.jar .
COPY --from=MAVEN_BUILD $HOME/config ./config

EXPOSE 8401/tcp
CMD java \
    -Xmx1024m -Djava.net.preferIPv4Stack=true  \
    -Dcom.sun.xml.bind.v2.runtime.JAXBContextImpl.fastBoot=true \
    -Djava.util.logging.config.file=/nsi-dds/config/logging.properties \
    -Dbasedir=/nsi-dds \
    -jar /nsi-dds/dds.jar