package info.andreaswolf.roadhopper.road


import scala.collection.mutable.ListBuffer

class RoadBendEvaluator {
	def findBend(roadSegments: List[RoadSegment]): List[RoadBend] = {
		var turnSum, arcLength = 0.0
		var segmentCount = 0

		val bends = new ListBuffer[RoadBend]

		// TODO this is inaccurate as we always start at the beginning of the current segment, no matter how far we’ve
		// progressed on it already
		var currentSegment = roadSegments.head
		var firstSegment = roadSegments.head
		roadSegments.tail.foreach(seg => {
			val angle: Double = currentSegment.calculateNecessaryTurn(seg)

			if (Math.abs(angle) >= 0.0) {//(2.0 * Math.PI / 180)) {
				if (Math.signum(turnSum) != Math.signum(angle) && Math.abs(turnSum) > 0.0 && arcLength > 0.0) {
					// turn ended; create RoadBend instance
					val direction = Math.signum(turnSum) match {
						case x if x == -1.0 => TurnDirection.LEFT
						case x if x == 1.0 => TurnDirection.RIGHT
					}

					bends append new RoadBend(arcLength, direction, turnSum, segmentCount, firstSegment)

					turnSum = 0.0
					arcLength = 0.0
					segmentCount = 0
					firstSegment = null
				}
				if (firstSegment == null) {
					firstSegment = seg
				}
				turnSum += angle
				arcLength += seg.length
				currentSegment = seg
				segmentCount += 1
			}
		})

		bends.toList
	}
}

/**
 * A bend in the road, i.e. a change of direction over two or more segments
 *
 * @param length The length of the arc
 * @param angle The turn angle in radians
 */
class RoadBend(val length: Double, val direction: TurnDirection.Value, val angle: Double, val segmentCount: Int,
               val firstSegment: RoadSegment) {

	lazy val radius = length / angle

	override def toString: String = f"RoadBend<$segmentCount: ${length.round} m, radius $radius%.2f m, $direction, $angle%.4f rad>"
}

object TurnDirection extends Enumeration {
	val LEFT, RIGHT = Value
}
