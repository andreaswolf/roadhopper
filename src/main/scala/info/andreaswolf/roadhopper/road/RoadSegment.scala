package info.andreaswolf.roadhopper.road

/**
 *
 * @param orientation The orientation in polar coordinates (-pi..+pi, 0 = east)
 */
class RoadSegment(val length: Float, var orientation: Double = 0.0) extends RoutePart {
	// correct orientation to be in (-pi..+pi]
	orientation %= (Math.PI * 2)
	if (orientation == -Math.PI)
		orientation = -orientation

	/**
	 * Returns the angle necessary to get from this segment to the given segment
	 *
	 * TODO check for u-turns—must be right or left depending on the country
	 */
	def calculateNecessaryTurn(nextSegment: RoadSegment): Double = {
		(nextSegment.orientation - orientation) % (Math.PI * 2)
	}
}
