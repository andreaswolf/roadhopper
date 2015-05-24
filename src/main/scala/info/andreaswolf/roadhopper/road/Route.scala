package info.andreaswolf.roadhopper.road

class Route(val parts: List[RoutePart]) {

	/**
	 * The length of the road
	 *
	 * @return
	 */
	def length: Float = getRoadSegments.map(_.length).sum

	/**
	 * @return All parts of the route that are road segments
	 */
	def getRoadSegments: List[RoadSegment] = {
		parts.collect { case b:RoadSegment => b }
	}

	def getSegmentForPosition(position: Double): RoadSegment = {
		if (position > length) {
			throw new IllegalArgumentException("Position " + position + " is after end of road")
		}

		var iterationPosition = 0.0
		for (segment <- getRoadSegments) {
			iterationPosition += segment.length
			if (iterationPosition >= position) {
				return segment
			}
		}
		getRoadSegments.reverse.head // TODO can this be done in a simpler fashion?
	}

}
