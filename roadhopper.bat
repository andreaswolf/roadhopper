rem do not display executed commands
@ECHO OFF

rem http://stackoverflow.com/a/2027211
setlocal enabledelayedexpansion

SET CP=-classpath .;roadhopper-0.1-SNAPSHOT-standalone.jar;lib/\*

IF NOT DEFINED OSMFILE (
	IF ""=="%1" (
		ECHO "Please configure an OSM file either via setting the OSMFILE environment variable or by passing it as the first argument to %0"
		EXIT /B 1
	) ELSE (
		IF EXIST "%1" (
			SET OSMFILE="%1"
		) ELSE (
			ECHO "OSM file %1 does not exist."
			EXIT /B 2
		)
	)
)


IF EXIST "config.properties" (
	SET CONFIGFILE="config.properties"
) ELSE (
	SET CONFIGFILE="config-example.properties"
)

SET OPT=!OPT! -Djetty.port=8989
rem the log4j config file will be searched in the classpath (see $CP above)
SET OPT=!OPT! -Dlog4j.configuration=log4j.properties
SET OPT=!OPT! -Dconfig.file=./application.conf
SET OPT=!OPT! -Dorientdb.config.file=./orientdb/orientdb-server-config.xml
SET OPT=!OPT! -DORIENTDB_HOME=./orientdb/

SET CONFIG=!CONFIG! config=./!CONFIGFILE!
SET CONFIG=!CONFIG! jetty.resourcebase=./webapp/
SET CONFIG=!CONFIG! graph.location=./osm.graph/
SET CONFIG=!CONFIG! osmreader.osm=!OSMFILE!
SET CONFIG=!CONFIG! graph.elevation.provider=srtm


java -Xmx2G -Xms2G !CP! !OPT! info.andreaswolf.roadhopper.server.RoadHopperServer !CONFIG!
