package info.andreaswolf.roadhopper.road

object RoadSegment {

	/**
	 * The earth’s radius as defined for WGS84.
	 */
	val R = 6371000

	/**
	 *
	 * @return A RoadSegment instance created from the coordinates
	 *
	 * Note that this uses an approximation for the length that treats the line segment as being orthogonal to the
	 * earth’s radius (Pythagorean approximation). This is generally considered safe for the short distances we have.
	 * <http://www.movable-type.co.uk/scripts/latlong.html>
	 */
	def fromCoordinates(lat1: Double, lon1: Double, lat2: Double, lon2: Double) : RoadSegment = {
		val phi1 = lat1.toRadians
		val phi2 = lat2.toRadians
		val deltaLambda = (lon2 - lon1).toRadians
		val x = deltaLambda * Math.cos((phi1+phi2)/2)
		val y = phi2 - phi1
		val length = Math.sqrt(x*x + y*y) * R
		val orientation = Math.atan2(Math.sin(lon2 - lon1) * Math.cos(lat2), Math.cos(lat1) * Math.sin(lat2) -
			Math.sin(lat1) * Math.cos(lat2) * Math.cos(lon2 - lon1)).toDegrees

		new RoadSegment(length, orientation)
	}

}

/**
 *
 * @param orientation The orientation in polar coordinates (-pi..+pi, 0 = east)
 */
class RoadSegment(val length: Double, var orientation: Double = 0.0) extends RoutePart {

	/**
	 * Returns the angle necessary to get from this segment to the given segment
	 *
	 * TODO check for u-turns—must be right or left depending on the country
	 */
	def calculateNecessaryTurn(nextSegment: RoadSegment): Double = {
		(nextSegment.orientation - orientation) % (Math.PI * 2)
	}

	override def toString = f"RoadSegment($length%.2f, $orientation%.2f°)"
}
