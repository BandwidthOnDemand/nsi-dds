#!/bin/bash

# resolve links - $0 may be a softlink
PRG="$0"

while [ -h "$PRG" ]; do
  ls=`ls -ld "$PRG"`
  link=`expr "$ls" : '.*-> \(.*\)$'`
  if expr "$link" : '/.*' > /dev/null; then
    PRG="$link"
  else
    PRG=`dirname "$PRG"`/"$link"
  fi
done

PRGDIR=`dirname "$PRG"`
BASEDIR=`cd "$PRGDIR" >/dev/null; pwd`

KEYSTORE=$BASEDIR/config/keystore.jks
TRUSTSTORE=$BASEDIR/config/truststore.jks
PASSWORD="changeit"

java -Xmx1025m -Djava.net.preferIPv4Stack=true  \
	-Dapp.home="$BASEDIR" \
	-Dbasedir="$BASEDIR" \
	-Djava.util.logging.config.file="$BASEDIR/config/logging.properties" \
	-Dcom.sun.xml.bind.v2.runtime.JAXBContextImpl.fastBoot=true \
        -Djersey.config.server.tracing.type=ALL \
        -Djersey.config.server.tracing.threshold=VERBOSE \
        -Djavax.net.debug=all \
        -Djavax.net.ssl.keyStore=$KEYSTORE \
        -Djavax.net.ssl.keyStorePassword=$PASSWORD \
        -Djavax.net.ssl.trustStore=$TRUSTSTORE \
        -Djavax.net.ssl.trustStorePassword=$PASSWORD \
	-jar target/dds.jar \
	-ddsConfigFile config/dds.xml \
	$*
