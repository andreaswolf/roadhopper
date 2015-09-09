/*
 * Copyright (c) 2015 Andreas Wolf
 *
 * See te LICENSE file in the project root for further copyright information.
 */

package info.andreaswolf.roadhopper.road

object Turn {
	def splitSegmentBeforeTurn(turn: Turn): Seq[RoadSegment] = {
		splitSegmentBeforeTurn(turn.from, turn.to)
	}

	def splitSegmentBeforeTurn(from: RoadSegment, to: RoadSegment): Seq[RoadSegment] = {
		val beforeTurnLength = Math.min(from.length, 5.0)

		// TODO move the speed limit to the velocity estimator, just include a turn-degrees indicator in the turn segment
		// instead. The estimator should then find an appropriate target velocity based on the driver preferences
		// The target velocity should also not be fixed, but instead depend on the allowed velocity on the segment before;
		// otherwise the slowdown might get too harsh e.g. on highway exits in Germany
		val speedLimit = Math.abs(from.calculateNecessaryTurn(to)).toDegrees match {
			case x if x > 110 => 7 / 3.6
			case x if x > 80 => 10 / 3.6
			case x if x > 60 => 15 / 3.6
			case x => from.speedLimit
		}

		if (from.length - beforeTurnLength < 1.0) {
			// if we would get a very short segment (< 1m) and the pre-turn segment, we skip the first segment
			return Seq(new PreTurnSegment(from.start, from.end, speedLimit))
		}

		// get the segment until 5 meters before the turn point
		val beforeTurn = RoadSegment.fromExisting(from, beforeTurnLength)
		// get the segment right before the turn point
		val turnBase = new PreTurnSegment(beforeTurn.end, from.end, speedLimit)

		// filter out a zero-length segment that might occur if the from-segment was shorter than the slowdown distance
		// of 5 meters
		Seq(beforeTurn, turnBase).filter(_.length > 0.0)
	}
}

class Turn(val from: RoadSegment, val to: RoadSegment) {

}
