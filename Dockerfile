FROM maven:3-openjdk-17 AS MAVEN_BUILD

ENV BUILD_HOME /home/safnari

ENV HOME /nsi-dds
WORKDIR $HOME
COPY . .
RUN mvn clean install -Dmaven.test.skip=true -Ddocker.nocache
 
FROM openjdk:17

ENV HOME /nsi-dds
#DEBUG_OPTS: "-Djavax.net.debug=ssl:handshake:verbose:keymanager:trustmanager -Djava.security.debug=access:stack:certpath"
ENV DEBUG_OPTS ""
ENV LOGBACK "/nsi-dds/config/logback.xml"

USER 1000:1000
WORKDIR $HOME
COPY --from=MAVEN_BUILD $HOME/target/nsi-dds-1.3.0-RELEASE.jar ./dds.jar
COPY --from=MAVEN_BUILD $HOME/target/lib ./lib
COPY --from=MAVEN_BUILD $HOME/config ./config

EXPOSE 8401/tcp
CMD java \
    -Xmx1024m -Djava.net.preferIPv4Stack=true  \
    -Dcom.sun.xml.bind.v2.runtime.JAXBContextImpl.fastBoot=true \
    -Djava.util.logging.config.file=/nsi-dds/config/logging.properties \
    -Dlogging.config=$LOGBACK \
    -Dlogback.configurationFile=$LOGBACK \
    $DEBUG_OPTS \
    -cp /nsi-dds/lib \
    -Dbasedir=/nsi-dds \
    -jar /nsi-dds/dds.jar
