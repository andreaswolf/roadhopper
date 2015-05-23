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

}
