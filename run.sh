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
export BASEDIR=`cd "$PRGDIR" >/dev/null; pwd`
export JAR=`ls "$BASEDIR"/target/nsi-dds-*.jar | head -n 1`

exec "$BASEDIR/scripts/entrypoint.sh" "$@"
