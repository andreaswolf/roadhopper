/*
 * Copyright (c) 2015 Andreas Wolf
 *
 * See te LICENSE file in the project root for further copyright information.
 */

package info.andreaswolf.roadhopper.server

import java.io.File
import javax.servlet.http.{HttpServlet, HttpServletRequest, HttpServletResponse}

import com.google.inject.Inject
import com.graphhopper.util.CmdArgs
import com.graphhopper.util.shapes.GHPoint3D
import info.andreaswolf.roadhopper.road.{RoadBuilder, RouteFactory}
import org.json.{JSONArray, JSONObject, JSONStringer}
import org.slf4j.LoggerFactory

import scala.collection.immutable.TreeSet
import scala.collection.mutable.ListBuffer
import scala.collection.{JavaConversions, mutable}
import scala.io.Source

/**
 * Servlet to convert data from the measurements done by HEV to a format usable for display in the map.
 */
class MeasurementsServlet extends HttpServlet {

	@Inject
	var args: CmdArgs = null

	class MeasurementPoint(val date: Long, val position: GHPoint3D, val velocity: Double, val orientation: Double)
		extends Ordered[MeasurementPoint] {
		// Required as of Scala 2.11 for reasons unknown - the companion to Ordered
		// should already be in implicit scope
		//import scala.math.Ordered.orderingToOrdered

		def compare(that: MeasurementPoint): Int = this.date compare that.date
	}

	class MeasurementFile(val lines: Iterator[String], val headerLine: String = null) {
		lines.next()

		lazy val groups = {
			val items = new ListBuffer[Set[MeasurementPoint]]()

			val buffer = TreeSet.newBuilder[MeasurementPoint]
			//
			var timeStandingStill = 0

			def endMeasurementGroup(): Unit = {
				val newSet = buffer.result()
				if (newSet.nonEmpty) {
					items.append(buffer.result())
				}
				buffer.clear()
				timeStandingStill = 0
			}

			for (line <- lines) {
				// using ";0,000;" as an indicator that the speed is 0
				if (line.indexOf(";0,000;") > -1) {
					timeStandingStill += 1
				} else {
					// vehicle did not move for more than ten seconds => start new measurement
					if (timeStandingStill > 10) {
						endMeasurementGroup()
					}
				}

				val Array(time, _latitude, _longitude, _velocity, _heading) = line.split(";").map(_.trim)
				// NOTE only some of our files had a velocity in knots; therefore, we assume km/h for now.
				val velocityKmh = _velocity.replace(",", ".").toDouble
				val latitude = _latitude.replace(",", ".").toDouble
				val longitude = _longitude.replace(",", ".").toDouble
				val orientation = _heading.replace(",", ".").toDouble.toRadians

				// only include one measurement per second
				if (time.indexOf(",") == -1 || time.split(",").apply(1).equals("00")) {
					// ignore slow movements for creating the road
					if (velocityKmh > 1.0) {
						handlePointForRoad(latitude, longitude, velocityKmh)
					}

					try {
						val date = (time.substring(0, 2).toLong * 3600 + time.substring(2, 4).toLong * 60 + time.substring(4, 6).toLong) * 1000

						buffer += new MeasurementPoint(date, new GHPoint3D(latitude, longitude, 0.0), velocityKmh, orientation)
					} catch {
						case ex: NumberFormatException =>
							log.error(s"Could not parse time '$time': ${ex.getMessage}")
					}
				}
			}
			endMeasurementGroup()

			items.toList
		}
	}


	var roadBuilder: Option[RoadBuilder] = None

	val measurements = new mutable.TreeSet[MeasurementPoint]()

	val log = LoggerFactory.getLogger("MeasurementsServlet")


	def getParameter(req: HttpServletRequest, name: String, default: String = "") = {
		val params: Array[String] = req.getParameterMap.get(name)
		if (params != null && params.length > 0) {
			params.apply(0)
		} else {
			default
		}
	}

	override def doGet(req: HttpServletRequest, resp: HttpServletResponse): Unit = {
		val filename = req.getParameter("name")
		val basePath = args.get("measurements.path", "./measurements/")

		if (filename == null) {
			val folder = new File(basePath)
			val jsonFiles = new JSONStringer
			jsonFiles.`object`().key("files").array()

			folder.listFiles().sorted.foreach({ f => jsonFiles.value(f.getName) })

			jsonFiles.endArray().endObject()
			resp.getWriter.append(jsonFiles.toString)
		} else {
			val source = Source.fromFile(basePath + filename).getLines()
/*
			val headerLine = source.take(1).next()

			for (line <- source) {
				val Array(time, _latitude, _longitude, _velocity, _heading) = line.split(";").map(_.trim)
				val velocityKmh = _velocity.replace(",", ".").toDouble * 1.852
				val latitude = _latitude.replace(",", ".").toDouble
				val longitude = _longitude.replace(",", ".").toDouble
				val orientation = _heading.replace(",", ".").toDouble.toRadians

				// only include one measurement per second
				if (time.indexOf(",") == -1 || time.split(",").apply(1).equals("00")) {
					// ignore slow movements for creating the road
					if (velocityKmh > 1.0) {
						handlePointForRoad(latitude, longitude, velocityKmh)
					}

					try {
						val date = (time.substring(0, 2).toLong * 3600 + time.substring(2, 4).toLong * 60 + time.substring(4, 6).toLong) * 1000

						measurements add (new MeasurementPoint(date, new GHPoint3D(latitude, longitude, 0.0), velocityKmh, orientation))
					} catch {
						case ex: NumberFormatException =>
							log.error(s"Could not parse time '$time': ${ex.getMessage}")
					}
				}
			}
*/
			val file = new MeasurementFile(source)

			val jsonContents = new JSONStringer().`object`()

			jsonContents.key("measurements").value(convertMeasurementsFile(file))

			jsonContents.key("road").value(convertRoadForFrontend())

			resp.getWriter.append(jsonContents.endObject().toString)
		}
	}


	def convertRoadForFrontend(): JSONObject = {
		val road = roadBuilder map {
			_.build
		} get
		val optimizedRoad = RouteFactory.simplifyRoadSegments(road, 15.0)

		val points: List[GHPoint3D] = road.map(_.end) :+ road.head.start

		val json = new JSONObject()
		json.put("road", JavaConversions.mapAsJavaMap(
			Map(
				"before optimization" -> road.size,
				"after optimization" -> optimizedRoad.size,
				"segments" -> JavaConversions.seqAsJavaList(points)
			)
		))
		json
	}

	def convertMeasurementsFile(measurementFile: MeasurementFile): JSONArray = {
		val json = new JSONArray()

		// the measurement groups
		for (measurement <- measurementFile.groups) {
			val obj = new JSONObject()
			// the measurements within one group
			for (datum <- measurement) {
				obj.put(datum.date.toString, new JSONObject(JavaConversions.mapAsJavaMap(Map(
					"position" -> datum.position,
					"speed" -> datum.velocity,
					"direction" ->datum.orientation
				))))
			}
			json.put(obj)
		}
		json
	}

	def handlePointForRoad(latitude: Double, longitude: Double, velocity: Double) = {
		val point = new GHPoint3D(latitude, longitude, 0.0)

		roadBuilder match {
			case None => roadBuilder = Some(new RoadBuilder(point))

			case _ => roadBuilder map {
				_.addSegment(point)
			}
		}
	}

}
