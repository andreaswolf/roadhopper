/*
 * Copyright (c) 2015 Andreas Wolf
 *
 * See te LICENSE file in the project root for further copyright information.
 */

package info.andreaswolf.roadhopper.server

import javax.servlet.http.{HttpServlet, HttpServletRequest, HttpServletResponse}

import com.graphhopper.util.shapes.GHPoint3D
import info.andreaswolf.roadhopper.road.{RouteFactory, RoadBuilder}
import org.json.JSONObject
import org.slf4j.LoggerFactory

import scala.collection.{JavaConversions, mutable}
import scala.io.Source

/**
 * Servlet to convert data from the measurements done by HEV to a format usable for display in the map.
 */
class MeasurementsServlet extends HttpServlet {

	class MeasurementPoint(val date: Long, val position: GHPoint3D, val velocity: Double, val orientation: Double)
		extends Ordered[MeasurementPoint] {
		// Required as of Scala 2.11 for reasons unknown - the companion to Ordered
		// should already be in implicit scope
		//import scala.math.Ordered.orderingToOrdered

		def compare(that: MeasurementPoint): Int = this.date compare that.date
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
		val file = getParameter(req, "name", "Dienstag_1") + ".txt"

		// TODO change this path
		val source = Source.fromFile("/home/aw/Studium/Diplomarbeit/Daten Miriam/Messdaten Ampera/" + file).getLines()

		val headerLine = source.take(1).next()

		for (line <- source) {
			val Array(time, _latitude, _longitude, _velocity, _heading) = line.split(";").map(_.trim)
			val velocityKmh = _velocity.replace(",", ".").toDouble * 1.852
			val latitude = _latitude.replace(",", ".").toDouble
			val longitude = _longitude.replace(",", ".").toDouble
			val orientation = _heading.replace(",", ".").toDouble.toRadians
			// TODO the orientation provided by the data set is bulls... derive it from the positions instead
			// (current position -> next position)

			val Array(_, milliseconds) = time.split(",")

			// only include one measurement per second
			if (milliseconds.equals("00")) {
				// ignore slow movements for creating the road
				if (velocityKmh > 1.0) {
					handlePointForRoad(latitude, longitude, velocityKmh)
				}

				val date = (time.substring(0, 2).toLong * 3600 + time.substring(2, 4).toLong * 60 + time.substring(4, 6).toLong) * 1000

				measurements add (new MeasurementPoint(date, new GHPoint3D(latitude, longitude, 0.0), velocityKmh, orientation))
			}
		}

		val jsonContents = new mutable.LinkedHashMap[String, Object]()

		jsonContents.put("measurements", convertMeasurements())

		convertRoadForFrontend(jsonContents)

		val json = new JSONObject(JavaConversions.mapAsJavaMap(jsonContents))
		resp.getWriter.append(json.toString)
	}


	def convertRoadForFrontend(jsonContents: mutable.LinkedHashMap[String, Object]): Unit = {
		val road = roadBuilder map { _.build } get
		val optimizedRoad = RouteFactory.simplifyRoadSegments(road, 15.0)

		val points: List[GHPoint3D] = road.map(_.end) :+ road.head.start

		jsonContents.put("road", JavaConversions.mapAsJavaMap(
			Map(
				"before optimization" -> road.size,
				"after optimization" -> optimizedRoad.size,
				"segments" -> JavaConversions.seqAsJavaList(points)
			)
		))
	}

	def convertMeasurements(): Object = {
		val serialized = new mutable.LinkedHashMap[Long, Object]()
		for (measurement <- measurements) {
			val stateInfo = new mutable.HashMap[String, Any]()

			stateInfo.put("position", measurement.position)
			stateInfo.put("speed", measurement.velocity)
			stateInfo.put("direction", measurement.orientation)
			serialized.put(measurement.date, JavaConversions.mutableMapAsJavaMap(stateInfo))
		}
		JavaConversions.mutableMapAsJavaMap(serialized)
	}

	def handlePointForRoad(latitude: Double, longitude: Double, velocity: Double) = {
		val point = new GHPoint3D(latitude, longitude, 0.0)

		roadBuilder match {
			case None => roadBuilder = Some(new RoadBuilder(point))

			case _ => roadBuilder map { _.addSegment(point) }
		}
	}

}
