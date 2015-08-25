package info.andreaswolf.roadhopper.road

import org.slf4j.LoggerFactory

import scala.collection.generic.SeqFactory
import scala.collection.mutable.ListBuffer


class RoadBendAnalyzer {

	val log = LoggerFactory.getLogger(this.getClass)

	def findBends(roadSegments: List[RoadSegment]): List[RoadBend] = {
		var turnSum = 0.0

		val bends = new ListBuffer[RoadBend]
		val currentBend = new ListBuffer[RoadSegment]

		roadSegments.sliding(2).foreach(segments => {
			val List(segA, segB) = segments

			val angle: Double = segA.calculateNecessaryTurn(segB)

			log.debug("turn: " + angle.formatted("%.2f"))

			// Note that we should avoid including overly long segments here. Try calculating the circle radius for each
			// single bend, when the radius is too high, ignore it
			// TODO make this check depend on the maximum speed on the road segment
			if (Math.abs(angle) >= (15.0 * Math.PI / 180)) {
				if (Math.signum(turnSum) != Math.signum(angle)) {
					// ignore small angles
					if (Math.abs(turnSum) > (10.0 * Math.PI / 180)) {
						// turn ended; create RoadBend instance
						bends append createRoadBend(currentBend)
					}

					currentBend.clear()
					currentBend append segA
					turnSum = 0.0
				}
				currentBend append segB
				turnSum += angle
			}
		})
		if (Math.abs(turnSum) > (10.0 * Math.PI / 180)) {
			// turn ended; create RoadBend instance
			bends append createRoadBend(currentBend)
		}

		bends.toList
	}

	protected def createRoadBend(bend: ListBuffer[RoadSegment]): RoadBend = {
		val segmentCount = bend.length
		var currentSegment = bend.head
		var angle = 0.0

		bend.tail.foreach(seg => {
			angle += currentSegment.calculateNecessaryTurn(seg)

			currentSegment = seg
		})

		var length: Double = 0.0
		if (bend.length > 2) {
			// ignore first and last segment, use the second and second last length twice instead to remove very long lines
			// before/after a bend, especially in urban areas
			// Note that this does not fix the problem of a slowly progressing bend with very long lines before
			// and/or after. We would need to calculate the circle’s radius for each small bend instead then, and remove
			// outliers
			length = (bend.tail.reverse.tail.reverse foldLeft 0.0)(_ + _.length)
				+ bend.tail.head.length + bend.reverse.tail.head.length
		} else {
			// very naïve approach: use the minimum of both segments’ length
			// this is not sufficient; we need to take into account the maximum allowed speed and derive a proper radius from
			// that. Otherwise, a sharp bend between two long straight road segments (e.g. a turn in a city) will be travelled
			// with an unlikely high velocity
			length = Math.min(bend.head.length, bend.apply(1).length)

			// the radius can be at maximum half the allowed speed in km/h (or 1.8 * speed [m/s])
			val speedLimit = bend.map(_.speedLimit).min
			if (speedLimit > 0) {
				length = Math.min(
					length,
					speedLimit / 2 * angle
				)
			}
		}

		val direction = angle match {
			case x if x < 0.0 => TurnDirection.LEFT
			case x if x > 0.0 => TurnDirection.RIGHT
		}

		new RoadBend(length, direction, angle, segmentCount, bend.head)
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

	lazy val radius = Math.abs(length / angle)

	override def toString: String = f"RoadBend<$segmentCount: ${length.round} m, radius $radius%.2f m, $direction, $angle%.4f rad>"
}

object TurnDirection extends Enumeration {
	val LEFT, RIGHT = Value
}
