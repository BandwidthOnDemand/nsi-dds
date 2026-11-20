FROM maven:3-eclipse-temurin-17@sha256:e5ecbc4df842ec3639d9e17d6be339659e8bcb47dddacb354c8aeea281b940b3 AS maven-build

ARG VERSION
ENV HOME=/nsi-dds
WORKDIR $HOME
COPY . .
RUN mvn versions:set -DnewVersion="${VERSION:?VERSION build argument is required}" -DgenerateBackupPoms=false \
    && mvn clean install -Dmaven.test.skip=true -Ddocker.nocache

FROM eclipse-temurin:17-jre@sha256:fb2cec8da84e3f086b59e29bf432d372e396fe0c247adcfb89303a28c2e313c0

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
