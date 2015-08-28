RoadHopper
==========

RoadHopper is a software for simulating a vehicle to get a driving cycle (time–velocity diagram). For this, a road 
network based on actual maps data is constructed and a simulation is run on that network.

The maps data is taken from the [OpenStreetMap](http://openstreetmap.org) project. It is processed using [GraphHopper]
(http://graphhopper.com), an open source routing service for OpenStreetMap.

This project was started during my diploma thesis at the [Hybrid Electric Vehicles research group](http://www.eti.kit.edu/1071.php)
of the Karlsruhe Institute of Technology. 


Running RoadHopper
------------------

To run RoadHopper, you need a custom fork of GraphHopper with a few adjustments necessary for RoadHopper. Also, grab an
extract from OpenStreetMap from one of the download pages listed at https://wiki.openstreetmap.org/wiki/Downloading_data.
You need either a .pbf or a .osm (XML) file.

Clone [my GraphHopper fork](https://github.com/andreaswolf/graphhopper) and check out the correct branch:
 
    git checkout -b roadhopper-base origin/roadhopper-base

Then, clone RoadHopper into the your GraphHopper directory

    graphopper $ git clone https://github.com/andreaswolf/roadhopper

Currently, there are no scripts to run the software, so you need to configure it in your IDE. Create a run configuration
with the following parameters:

  * Main class: info.andreaswolf.roadhopper.server.RoadHopperServer
  * VM options: -Djetty.port=8989 -Dconfig.file=./roadhopper/application.conf -Dorientdb.config.file=./roadhopper/database/orientdb-server-config.xml -DORIENTDB_HOME=./roadhopper/
  * Program arguments: jetty.resourcebase=./web/src/main/webapp/ config=./config.properties graph.location=&lt;Path to your data directory&gt; osmreader.osm=&lt;Path to your OSM file&gt; graph.elevation.provider=srtm
  * Working directory: _Path to GraphHopper_

Keep in mind to replace the paths in &lt;&gt; with the proper locations.

What is also necessary is replacing the dependencies to the com.graphhopper.* modules by references to the modules inside
the IDE workspace.

When starting the software, GraphHopper will process the OSM file and create its internal data structure. Afterwards,
head to http://localhost:8989 and select two points. Then, a simulation will run – currently, a very rough estimate.
I’m working on that ;-). Afterwards, you see a t–v diagram at the bottom right.


Troubleshooting
---------------

tbd.


Development
-----------

tbd.

If you have any feature requests or need customization of RoadHopper to your use case, feel free to contact me at
dev (at) a-w dot io.


License
-------

This software is licensed under the MIT license. See the LICENSE file for more information.
