package info.andreaswolf.roadhopper.simulation

import akka.actor.{ActorLogging, ActorRef, Actor}
import akka.pattern.ask
import info.andreaswolf.roadhopper.road.{RoadSegment, Route}

import scala.collection.mutable.ListBuffer
import scala.concurrent.{Future, ExecutionContext}
import scala.util.Success

case class RequestRoadAhead(position: Int)

case class RoadAhead(time: Int, roadParts: List[RoadSegment])


class TwoStepJourneyActor(val timer: ActorRef, val vehicle: ActorRef, val route: Route)
	extends SimulationActor with ActorLogging {

	var remainingSegments = route.parts.tail
	var currentSegment = route.parts.head
	var travelledUntilCurrentSegment = 0.0

	var currentTime = 0

	var active = true

	registerReceiver({
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
	})

	/**
	 * Handler for [[Start]] messages.
	 * <p/>
	 * The simulation will only continue after the Future has been completed. You can, but don’t need to override this
	 * method in your actor. If you don’t override it, the step will be completed immediately (by the successful Future
	 * returned)
	 */
	override def start()(implicit exec: ExecutionContext): Future[Any] = timer ? ScheduleStep(10, self)

	/**
	 * Handler for [[StepAct]] messages.
	 * <p/>
	 * The simulation will only continue after the Future has been completed. You can, but don’t need to override this
	 * method in your actor. If you don’t override it, the step will be completed immediately (by the successful Future
	 * returned)
	 *
	 * @param time The current simulation time in milliseconds
	 */
	override def stepAct(time: Int)(implicit exec: ExecutionContext): Future[Any] = {
		if (!active) {
			return Future.successful()
		}
		val futures = new ListBuffer[Future[Any]]()

		futures.append(vehicle ? GetStatus() andThen {
			case Success(VehicleStatus(statusTime, state, travelledDistance)) =>
				if (!checkCurrentSegment(travelledDistance)) {
					active = false
				}
		})
		// only schedule if the journey has not ended
		futures.append(timer ? ScheduleStep(time + 10, self))

		Future.sequence(futures.toList)
	}

	/**
	 * Checks if we are still on the current segment or if we moved beyond it and need to adjust the segment
	 * and the vehicle orientation
	 *
	 * @param position The current position on the journey, i.e. along the road to travel
	 * @return true if we are still within the road to travel, false if the journey has ended
	 */
	def checkCurrentSegment(position: Double): Boolean = {
		// are we beyond the current segment’s end?
		if (position <= travelledUntilCurrentSegment + currentSegment.length) {
			return true
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

			true
		} else {
			log.info("Journey ended after " + travelledUntilCurrentSegment + " (not accurate!)")
			// the shutdown will only be executed when all existing messages have been processed; therefore, we only tell the
			// timer to stop, but leave shutting down the system up to it
			timer ! Stop()
			false
		}
	}

}

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
