package info.andreaswolf.roadhopper.road

import com.graphhopper.util.shapes.GHPoint3D

/**
 * Test helper class for construction road segments
 */
object RoadSegmentHelper {

	/**
	 * The earth’s radius as defined for WGS84.
	 */
	val R = 6371000


	/**
	 * Creates a road segment from a start point with the given length, pointing in the given direction.
	 *
	 * @param orientation The orientation in radians
	 * @return
	 */
	def fromPoint(start: GHPoint3D, length: Double, orientation: Double): RoadSegment = {
		// see http://www.movable-type.co.uk/scripts/latlong.html
		val startLat = start.lat.toRadians
		val startLon = start.lon.toRadians

		val newLat = Math.asin(Math.sin(startLat) * Math.cos(length / R) +
			Math.cos(startLat) * Math.sin(length / R) * Math.cos(orientation))
		val newLon = startLon + Math.atan2(Math.sin(orientation) * Math.sin(length / R) * Math.cos(startLat),
			Math.cos(length / R) - Math.sin(startLat) * Math.sin(newLat))

		val end = new GHPoint3D(newLat.toDegrees, newLon.toDegrees, start.ele)

		new RoadSegment(start, end)
	}

}
