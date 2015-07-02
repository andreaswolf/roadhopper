package info.andreaswolf.roadhopper.simulation

import akka.actor.{ActorLogging, ActorRef, Actor}
import info.andreaswolf.roadhopper.road.{RoadSegment, Route}

import scala.collection.mutable.ListBuffer

case class RequestRoadAhead(position: Int)

case class RoadAhead(time: Int, roadParts: List[RoadSegment])

class JourneyActor(val timer: ActorRef, val vehicle: ActorRef, val route: Route) extends Actor with ActorLogging {

	var remainingSegments = route.parts
	var currentSegment = remainingSegments.head
	var travelledUntilCurrentSegment = 0.0

	var currentTime = 0

	override def receive: Receive = {
		case Start() =>
			log.info("Journey started")
			timer ! ScheduleRequest(10)

		case Step(time) =>
			currentTime = time
			if (remainingSegments.nonEmpty) {
				vehicle ! RequestVehicleStatus()
			} else {
				log.info("Journey ended after " + travelledUntilCurrentSegment + " (not accurate!)")
				timer ! Pass
				context.system.shutdown
			}

		case RequestRoadAhead(position) =>
			// TODO dynamically calculate the base distance to get (e.g. based on speed) or get it passed with the request
			// make sure we only get segments after the current segment
			val remainingOnCurrentSegment = currentSegment.length - (position - travelledUntilCurrentSegment)
			// if the length to get is 0, we will be on the current segment for all of the look-ahead distance
			var lengthToGet = Math.max(0, 150.0 - remainingOnCurrentSegment)

			val offsetOnCurrentSegment = position - travelledUntilCurrentSegment

			val segmentsAhead = new ListBuffer[RoadSegment]
			segmentsAhead append RoadSegment.fromExisting(offsetOnCurrentSegment, currentSegment)
			remainingSegments.foreach(segment => {
				if (lengthToGet > 0) {
					segmentsAhead append segment
					lengthToGet -= segment.length
				}
			})
			sender ! new RoadAhead(currentTime, segmentsAhead.toList)

		case VehicleStatus(time, state, travelledDistance) =>
			if (remainingSegments.nonEmpty && travelledDistance > travelledUntilCurrentSegment + currentSegment.length) {
				// TODO this brings a slight inaccuracy into the calculation, which will lead to longer travelling
				// distances. The difference is negligible for long segments, but for many consecutive short segments,
				// we might get a larger offset
				travelledUntilCurrentSegment = travelledDistance

				currentSegment = remainingSegments.head
				remainingSegments = remainingSegments.tail
				if (remainingSegments.nonEmpty) {
					val nextSegment = remainingSegments.head

					vehicle ! Turn(currentSegment.calculateNecessaryTurn(nextSegment))
				}
				log.debug("RoadSegment ended, new segment length: " + currentSegment.length.round)
				log.debug("Remaining segments: " + remainingSegments.length)
			}
			timer ! ScheduleRequest(currentTime + 10)

	}
}
