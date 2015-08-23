package info.andreaswolf.roadhopper.road

import com.graphhopper.util.shapes.GHPoint3D

import scala.collection.mutable.ListBuffer


class RoadBuilder(val start: GHPoint3D) {

	val segments = new ListBuffer[RoadSegment]()


	def addSegment(length: Double, orientation: Double): RoadBuilder = {
		val radians = orientation.toRadians

		if (segments.isEmpty) {
			segments append RoadSegment.fromPoint(start, length, radians)
		} else {
			segments append RoadSegment.fromPoint(segments.last.end, length, radians)
		}

		this
	}

	def addSegment(end: GHPoint3D): RoadBuilder = {
		if (segments.isEmpty) {
			if (start.equals(end)) {
				return this
			}
			segments append RoadSegment.fromPoints(start, end)
		} else {
			if (segments.last.end.equals(end)) {
				return this
			}
			segments append RoadSegment.fromPoints(segments.last.end, end)
		}

		this
	}

	def build = segments.toList

	def buildRoute = new Route(segments.toList)

}
