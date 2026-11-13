FROM maven:3-eclipse-temurin-17@sha256:0362bf77ae7b21e3e0a0f4b211e85eb973e2cf9a680b2d36210e580336bb2255 AS maven-build

ARG VERSION
ENV HOME=/nsi-dds
WORKDIR $HOME
COPY . .
RUN mvn versions:set -DnewVersion="${VERSION:?VERSION build argument is required}" -DgenerateBackupPoms=false \
    && mvn clean install -Dmaven.test.skip=true -Ddocker.nocache

FROM eclipse-temurin:17-jre@sha256:0b6521287f850ec1c4b31c9074c5a1ae1d67c5922c3fefd21065c7c32e244d4a

ENV BASEDIR=/nsi-dds
ENV CONFIG_DIR=$BASEDIR/config
# DEBUG_OPTS example:
# -Djavax.net.debug=ssl:handshake:verbose:keymanager:trustmanager -Djava.security.debug=access:stack:certpath
ENV DEBUG_OPTS=""
ENV JAVA_OPTS=""
ENV JAVA_HEAP="-Xmx1024m"

USER 1000:1000
WORKDIR $BASEDIR
COPY --chown=1000:1000 --from=maven-build $BASEDIR/target/nsi-dds*.jar ./dds.jar
COPY --chown=1000:1000 --from=maven-build $BASEDIR/target/lib ./lib
COPY --chown=1000:1000 --from=maven-build $BASEDIR/config ./config
COPY --chown=1000:1000 --from=maven-build $BASEDIR/scripts/entrypoint.sh ./entrypoint.sh

EXPOSE 8401/tcp
ENTRYPOINT ["/nsi-dds/entrypoint.sh"]
