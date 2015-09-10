package info.andreaswolf.roadhopper.road

import com.graphhopper.util.shapes.GHPoint3D

import scala.collection.mutable.ListBuffer


class RoadBuilder(val start: GHPoint3D) {

	val segments = new ListBuffer[RoadSegment]()


	var analyzeTurns = false
	private val _roadBendAnalyzer = new RoadBendAnalyzer()


	/**
	 * Adds a segment with the given length and orientation.
	 *
	 * @param length The length in meters
	 * @param orientation The orientation of the new segment in degrees
	 */
	def addSegment(length: Double, orientation: Double): RoadBuilder = {
		val radians = orientation.toRadians

		if (segments.isEmpty) {
			segments append RoadSegment.fromPoint(start, length, radians)
		} else {
			segments append RoadSegment.fromPoint(segments.last.end, length, radians)
		}

		this
	}

	/**
	 * Adds a segment with the defined end point
	 */
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

	def enableTurnAnalysis() = {
		analyzeTurns = true

		this
	}

	def build() = {
		if (analyzeTurns) {
			_roadBendAnalyzer.markTurns(segments.toList)
		} else {
			segments.toList
		}
	}

	def buildRoute = new Route(build())

}
