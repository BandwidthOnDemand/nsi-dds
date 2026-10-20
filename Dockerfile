FROM maven:3-eclipse-temurin-17@sha256:a6d08f0b65a5420d67425b9851283b6b6f7f49b4b3cdfb234a4203bc59784124 AS maven-build

ARG VERSION
ENV HOME=/nsi-dds
WORKDIR $HOME
COPY . .
RUN mvn versions:set -DnewVersion="${VERSION:?VERSION build argument is required}" -DgenerateBackupPoms=false \
    && mvn clean install -Dmaven.test.skip=true -Ddocker.nocache

FROM eclipse-temurin:17-jre@sha256:e04fa72be13b257a59fdec6dc14af78c8f80f035d3c1ed4e03e45552a231d4c8

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
