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
		val speedLimit = Math.abs(from.calculateNecessaryTurn(to)).toDegrees match {
			case x if x > 110 => 7
			case x if x > 80 => 10
			case x if x > 45 => 15
			case x if x > 30 => 25
			case x => from.speedLimit
		}

		// get the segment until 5 meters before the turn point
		val beforeTurn = RoadSegment.fromExisting(from, beforeTurnLength)
		// get the segment right before the turn point
		val turnBase = new PreTurnSegment(beforeTurn.end, from.end, speedLimit / 3.6)

		Seq(beforeTurn, turnBase)
	}
}

class Turn(val from: RoadSegment, val to: RoadSegment) {

}
