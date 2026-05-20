#!/bin/sh
set -e

: "${BASEDIR:=/nsi-dds}"
: "${CONFIG_DIR:=$BASEDIR/config}"
: "${JAR:=$BASEDIR/dds.jar}"
: "${JAVA_HEAP:=-Xmx1024m}"
: "${LOGBACK:=$CONFIG_DIR/logback.xml}"

exec java \
  $JAVA_HEAP \
  -Djava.net.preferIPv4Stack=true \
  -Dcom.sun.xml.bind.v2.runtime.JAXBContextImpl.fastBoot=true \
  -Djdk.tls.client.enableSessionTicketExtension=false \
  -Djava.util.logging.config.file="$CONFIG_DIR/logging.properties" \
  -Dlogging.config="$LOGBACK" \
  -Dlogback.configurationFile="$LOGBACK" \
  -Dbasedir="$BASEDIR" \
  $JAVA_OPTS \
  $DEBUG_OPTS \
  -jar "$JAR" "$@"
