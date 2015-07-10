package info.andreaswolf.roadhopper.simulation

import akka.actor.{ActorLogging, ActorRef, Actor}
import info.andreaswolf.roadhopper.road.{RoadSegment, Route}

import scala.collection.mutable.ListBuffer

case class RequestRoadAhead(position: Int)

case class RoadAhead(time: Int, roadParts: List[RoadSegment])

class JourneyActor(val timer: ActorRef, val vehicle: ActorRef, val route: Route) extends Actor with ActorLogging {

	var remainingSegments = route.parts.tail
	var currentSegment = route.parts.head
	var travelledUntilCurrentSegment = 0.0

	var currentTime = 0

	override def receive: Receive = {
		case Start() =>
			log.info("Journey started")
			timer ! ScheduleRequest(10)

		case Step(time) =>
			currentTime = time
			vehicle ! RequestVehicleStatus()

		case RequestRoadAhead(position) =>
			// TODO dynamically calculate the distance to get (e.g. based on speed) or get it passed with the request
			// check if we have probably advanced past the current segment
			checkCurrentSegment(position)

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
			sender ! RoadAhead(currentTime, segmentsAhead.toList)
			// inform the vehicle about its current position (= the start of the first road segment ahead)
			vehicle ! UpdatePosition(segmentsAhead.head.start)

		// TODO move tracking the travelled distance here, as the vehicle should not need to be concerned with it
		case VehicleStatus(time, state, travelledDistance) =>
			checkCurrentSegment(travelledDistance)
			timer ! ScheduleRequest(currentTime + 10)

	}

	/**
	 * Checks if we are still on the current segment or if we moved beyond it and need to adjust the segment
	 * and the vehicle orientation
	 *
	 * @param position The current position on the journey, i.e. along the road to travel
	 */
	def checkCurrentSegment(position: Double): Unit = {
		// are we beyond the current segment’s end?
		if (position <= travelledUntilCurrentSegment + currentSegment.length) {
			return
		}

		// there are more segments ahead, so just continue with the next one
		if (remainingSegments.nonEmpty) {
			// TODO this brings a slight inaccuracy into the calculation, which will lead to longer travelling
			// distances. The difference is negligible for long segments, but for many consecutive short segments,
			// we might get a larger offset
			travelledUntilCurrentSegment = position

			val nextSegment = remainingSegments.head

			// instruct the vehicle to turn to the new segment
			vehicle ! Turn(currentSegment.calculateNecessaryTurn(nextSegment))

			currentSegment = nextSegment
			remainingSegments = remainingSegments.tail

			log.debug("RoadSegment ended, new segment length: " + currentSegment.length.round)
			log.debug("Remaining segments: " + remainingSegments.length)
		} else {
			log.info("Journey ended after " + travelledUntilCurrentSegment + " (not accurate!)")
			timer ! Pass
			context.system.shutdown
		}
	}
}
