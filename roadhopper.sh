#!/bin/sh

# the current directory is included because the log4j properties won't be found otherwise
CP="-classpath .:roadhopper-0.1-SNAPSHOT-standalone.jar:lib/\*"

if [ "$OSMFILE" == "" ]; then
	if [ "" == "$1" ]; then
		echo "Please configure an OSM file either via setting the OSMFILE environment variable or by passing it as the first argument to $0"
		exit 1
	else
		if [ -f "$1" ]; then
			OSMFILE=$1
		else
			echo "OSM file $1 does not exist."
			exit 2
		fi
	fi
fi

if [ -f "config.properties" ]; then
	CONFIGFILE="config.properties"
else
	CONFIGFILE="config-example.properties"
fi

OPT="$OPT -Djetty.port=8989"
# the log4j config file will be searched in the classpath (see $CP above)
OPT="$OPT -Dlog4j.configuration=log4j.properties"
OPT="$OPT -Dconfig.file=./application.conf"
OPT="$OPT -Dorientdb.config.file=./orientdb/orientdb-server-config.xml"
OPT="$OPT -DORIENTDB_HOME=./orientdb/"
CONFIG="$CONFIG config=./$CONFIGFILE"
CONFIG="$CONFIG jetty.resourcebase=./webapp/"
CONFIG="$CONFIG graph.location=./osm.graph/"
CONFIG="$CONFIG osmreader.osm=$OSMFILE"
CONFIG="$CONFIG graph.elevation.provider=srtm"


java -Xmx2G -Xms2G $CP $OPT info.andreaswolf.roadhopper.server.RoadHopperServer $CONFIG
