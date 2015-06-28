package info.andreaswolf.roadhopper.simulation

import akka.actor.{ActorRef, Actor}
import info.andreaswolf.roadhopper.road.{RoadSegment, Route}

class JourneyActor(val timer: ActorRef, val vehicle: ActorRef, val route: Route) extends Actor {

	var remainingSegments = route.getRoadSegments
	var travelledUntilCurrentSegment = 0.0

	var currentTime = 0

	override def receive: Receive = {
		case Start() =>
			println("Journey started")
			timer ! ScheduleRequest(10)

		case Step(time) =>
			if (remainingSegments.nonEmpty) {
				vehicle ! RequestVehicleStatus()
				currentTime = time
			} else {
				println("Journey ended after " + travelledUntilCurrentSegment + " (not accurate!)")
				timer ! Pass
			}

		case VehicleStatus(time, state, travelledDistance) =>
			if (remainingSegments.nonEmpty && travelledDistance > travelledUntilCurrentSegment + remainingSegments.head.length) {
				// TODO this brings a slight inaccuracy into the calculation, which will lead to longer travelling
				// distances. The difference is negligible for long segments, but for many consecutive short segments,
				// we might get a larger offset
				travelledUntilCurrentSegment = travelledDistance

				val currentSegment = remainingSegments.head
				remainingSegments = remainingSegments.tail
				if (remainingSegments.nonEmpty) {
					val nextSegment = remainingSegments.head

					vehicle ! Turn(currentSegment.calculateNecessaryTurn(nextSegment))
				}
				println("RoadSegment ended, new segment length: " + remainingSegments.length)
				println("Remaining segments: " + remainingSegments.length)
			}
			timer ! ScheduleRequest(currentTime + 10)

	}
}
