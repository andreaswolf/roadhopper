package info.andreaswolf.roadhopper.road

import com.graphhopper.util.PointList

/**
 * A route used as the basis for a simulation.
 *
 * @param parts The road segments this route consists of. Strictly ordered.
 */
class Route(val parts: List[RoadSegment]) {

	/**
	 * The identifier for this route, used to track it across requests.
	 *
	 * Note that this is not guaranteed to be globally unique, but for our purpose a session-unique identifier is fully
	 * sufficient.
	 */
	val identifier: String = java.lang.Long.toHexString(new java.util.Date().getTime)

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

	def getPointList: PointList = {
		val list = new PointList()
		list.add(parts.head.start)
		parts.foreach(part => list.add(part.end))
		list
	}

}
