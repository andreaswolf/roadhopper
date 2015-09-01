#!/bin/sh

CP="-classpath roadhopper-0.1-SNAPSHOT-standalone.jar:lib/\*"

if [ "$OSMFILE" == "" ]; then
	if [ "" == "$1" ]; then
		echo "Please configure an OSM file either via setting the OSMFILE environment variable or by passing it as the first argument to $0"
		exit 1
	else
		# TODO check if file really exists
		OSMFILE=$1
	fi
fi

OPT="$OPT -Djetty.port=8989"
OPT="$OPT -Dlog4j.configuration=log4j.properties"
OPT="$OPT -Dconfig.file=./application.conf"
OPT="$OPT -Dorientdb.config.file=./orientdb/orientdb-server-config.xml"
OPT="$OPT -DORIENTDB_HOME=./orientdb/"
CONFIG="$CONFIG config=./config.properties"
CONFIG="$CONFIG jetty.resourcebase=./webapp/"
CONFIG="$CONFIG graph.location=./osm.graph/"
CONFIG="$CONFIG osmreader.osm=$OSMFILE"
CONFIG="$CONFIG graph.elevation.provider=srtm"


java -Xmx2G -Xms2G $CP $OPT info.andreaswolf.roadhopper.server.RoadHopperServer $CONFIG
