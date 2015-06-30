package info.andreaswolf.roadhopper.road

class Route(val parts: List[RoadSegment]) {

	/**
	 * The length of the road
	 *
	 * @return
	 */
	def length: Double = getRoadSegments.map(_.length).sum

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

	/**
	 * Returns a list of (x, y) pairs for the road segment points.
	 */
	def roadSegmentCoordinates: List[(Double, Double)] = {
		val coordinates = getRoadSegments map ((s: RoadSegment) => {
			val angle = s.orientation
			val x = Math.cos(angle) * s.length
			val y = Math.sin(angle) * s.length
			new Tuple2(x, y)
		})

		new Tuple2(0.0, 0.0) :: coordinates
	}

}
