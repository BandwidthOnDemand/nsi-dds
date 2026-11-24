FROM maven:3-eclipse-temurin-17@sha256:01dbb85e5c7236970e0a3b5dc47994af3f5774e2bb0a11156b905a8f4c68ff96 AS maven-build

ARG VERSION
ENV HOME=/nsi-dds
WORKDIR $HOME
COPY . .
RUN mvn versions:set -DnewVersion="${VERSION:?VERSION build argument is required}" -DgenerateBackupPoms=false \
    && mvn clean install -Dmaven.test.skip=true -Ddocker.nocache

FROM eclipse-temurin:17-jre@sha256:541b08fc7f494edf14e436ff6bfb55767e4a115c87bfc547b8c5de957b58737f

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
