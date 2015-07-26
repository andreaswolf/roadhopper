package info.andreaswolf.roadhopper.road

import com.graphhopper.util.shapes.GHPoint3D

import scala.collection.mutable.ListBuffer


class RoadBuilder(val start: GHPoint3D) {

	val segments = new ListBuffer[RoadSegment]()


	def addSegment(length: Double, orientation: Double): RoadBuilder = {
		val radians = orientation.toRadians

		if (segments.isEmpty) {
			segments append RoadSegmentHelper.fromPoint(start, length, radians)
		} else {
			segments append RoadSegmentHelper.fromPoint(segments.last.end, length, radians)
		}

		this
	}

	def build = segments.toList

	def buildRoute = new Route(segments.toList)

}
