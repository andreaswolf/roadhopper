/*
 * Copyright (c) 2015 Andreas Wolf
 *
 * See te LICENSE file in the project root for further copyright information.
 */

package info.andreaswolf.roadhopper.measurements

import com.graphhopper.util.StopWatch
import com.graphhopper.util.shapes.GHPoint3D
import info.andreaswolf.roadhopper.map.Point
import info.andreaswolf.roadhopper.road.{RoadBuilder, RoadSegment, RouteFactory}
import org.json.JSONObject
import org.slf4j.LoggerFactory

import scala.collection.JavaConversions
import scala.collection.mutable.ListBuffer

class MeasurementFile(val name: String, val lines: Iterator[String], val headerLine: String = null) {

	var roadBuilder: Option[RoadBuilder] = None

	val log = LoggerFactory.getLogger(this.getClass)


	lines.next()

	lazy val measurements = {
		val items = new ListBuffer[Measurement]()

		val buffer = new ListBuffer[DataPoint]()
		//
		var timeSinceStopping = 0

		var min: Long = Long.MaxValue
		var max: Long = 0
		var sum: Long = 0
		var group = 0
		val sw = new StopWatch().start()
		val sw2 = new StopWatch().start()
		def endMeasurementGroup(): Unit = {
			val newSet = buffer
			if (newSet.nonEmpty) {
				if (roadBuilder.isDefined && roadBuilder.get.segments.nonEmpty) {
					items.append(new Measurement(name + "_" + group, buffer.toList, roadBuilder.map(_.build).get))
				}
				roadBuilder = None
				group += 1
			}
			buffer.clear()
			timeSinceStopping = 0
		}

		var c = 0
		for (line <- lines) {
			// using ";0.000;" as an indicator that the speed is 0
			if (line.indexOf(";0.000;") > -1) {
				timeSinceStopping += 1
			} else {
				// vehicle did not move for more than ten seconds => start new measurement
				if (timeSinceStopping > 10) {
					log.debug(s"Starting new measurement group after line $c")
					endMeasurementGroup()
				}
			}
			c += 1
			if (c % 1000 == 0) {
				sw.stop()
				log.debug(s"Handled $c lines - ${sw.getTime} (min/avg/max: $min/${sum / 1000}/$max)")
				// restart stop watch
				sw.reset().start()
				min = Long.MaxValue
				max = 0
				sum = 0
			}

			val Array(time, _latitude, _longitude, _velocity, _heading) = line.split(";").map(_.trim)
			// NOTE only some of our files had a velocity in knots; therefore, we assume km/h for now.
			val velocityKmh = _velocity.replace(",", ".").toDouble
			val latitude = _latitude.replace(",", ".").toDouble
			val longitude = _longitude.replace(",", ".").toDouble
			val orientation = _heading.replace(",", ".").toDouble.toRadians

			// only include one measurement per second
			if (time.indexOf(",") == -1 || time.split(",").apply(1).equals("00")) {
				sw2.reset().start()

				// ignore slow movements for creating the road
				if (velocityKmh > 1.0) {
					handlePointForRoad(latitude, longitude, velocityKmh)
				}

				try {
					val date = (time.substring(0, 2).toLong * 3600 + time.substring(2, 4).toLong * 60
						+ time.substring(4, 6).toLong) * 1000 + time.substring(7, 9).toLong * 10

					buffer += DataPoint(date, Point(latitude, longitude, 0.0), velocityKmh / 3.6, orientation)
				} catch {
					case ex: NumberFormatException =>
						log.error(s"Could not parse time '$time': ${ex.getMessage}")
				}
				sw2.stop().getNanos match {
					case x if x > max => max = x
					case x if x < min => min = x
					case x =>
				}
				sum += sw2.getNanos
			}
		}
		endMeasurementGroup()

		log.debug(s"Read file ${name}")

		items.toList
	}

	def handlePointForRoad(latitude: Double, longitude: Double, velocity: Double) = {
		val point = Point(latitude, longitude)

		roadBuilder match {
			case None => roadBuilder = Some(new RoadBuilder(point))

			case _ => roadBuilder map {
				_.addSegment(point)
			}
		}
	}

	def convertRoadForFrontend(): JSONObject = {
		val road = roadBuilder.map(_.build).get
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

}
