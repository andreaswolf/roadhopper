package info.andreaswolf.roadhopper.simulation

import akka.actor.{ActorLogging, ActorRef, Actor}
import akka.pattern.ask
import com.graphhopper.util.shapes.GHPoint3D
import info.andreaswolf.roadhopper.road.{RoadSegment, Route}

import scala.collection.mutable.ListBuffer
import scala.concurrent.{Future, ExecutionContext}
import scala.util.Success

case class RequestRoadAhead(position: Int)

case class RoadAhead(time: Int, roadParts: List[RoadSegment], isEnding: Boolean = false) {
	def length: Double = roadParts.map(_.length).sum
}


class TwoStepJourneyActor(val timer: ActorRef, val vehicle: ActorRef, val route: Route)
	extends SimulationActor with ActorLogging {

	val length = route.parts.map(_.length).sum

	var remainingSegments = route.parts.tail
	var currentSegment = route.parts.head
	var travelledUntilCurrentSegment = 0.0

	var currentTime = 0

	var currentPosition: Option[GHPoint3D] = None

	var active = true

	registerReceiver({
		case RequestRoadAhead(travelledDistance) =>
			// TODO dynamically calculate the distance to get (e.g. based on speed) or get it passed with the request
			// check if we have probably advanced past the current segment
			checkCurrentSegment(travelledDistance)

			// make sure we only get segments after the current segment
			val remainingOnCurrentSegment = currentSegment.length - (travelledDistance - travelledUntilCurrentSegment)
			// if the length to get is 0, we will be on the current segment for all of the look-ahead distance
			var lengthToGet = Math.max(0, 150.0 - remainingOnCurrentSegment)

			val offsetOnCurrentSegment = travelledDistance - travelledUntilCurrentSegment

			val segmentsAhead = new ListBuffer[RoadSegment]
			segmentsAhead append RoadSegment.fromExisting(offsetOnCurrentSegment, currentSegment)
			currentPosition = Some(segmentsAhead.head.start)
			remainingSegments.foreach(segment => {
				if (lengthToGet > 0) {
					segmentsAhead append segment
					lengthToGet -= segment.length
				}
			})
			// if there are no more journey parts left after the current ones, this journey will end
			val journeyEndsAfterFilteredSegments: Boolean = remainingSegments.length == segmentsAhead.length - 1

			sender ! RoadAhead(currentTime, segmentsAhead.toList, journeyEndsAfterFilteredSegments)
			log.debug(f"Travelled until here: $travelledDistance, LengthToGet: $lengthToGet%.2f;" +
				f" got length: ${segmentsAhead.toList.map(_.length).sum}%.2f;" +
				f" segments: ${segmentsAhead.length - 1}/${remainingSegments.length}")
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
	 * Handler for [[StepUpdate]] messages.
	 * <p/>
	 * The simulation will only continue after the Future has been completed. You can, but don’t need to override this
	 * method in your actor. If you don’t override it, the step will be completed immediately (by the successful Future
	 * returned)
	 *
	 * @param time The current simulation time in milliseconds
	 */
	override def stepUpdate(time: Int)(implicit exec: ExecutionContext): Future[Any] = Future {
		currentTime = time
		if (currentPosition.isDefined) {
			vehicle ? UpdatePosition(currentPosition.get)
		} else {
			Future.successful()
		}
	}


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

		val statusFuture: Future[JourneyStatus] = (vehicle ? GetStatus()).asInstanceOf[Future[JourneyStatus]]
		futures.append(statusFuture)

		// react to the journey status we got
		futures.append(statusFuture flatMap { status: JourneyStatus =>
				Future {
					log.debug(f"to travel: ${Math.abs(status.travelledDistance - length)}%.2f")

					if (status.vehicleState.speed == 0.0 && Math.abs(status.travelledDistance - length) < 1.5) {
						active = false
						timer ! Stop()
					}
				}
		})
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
			travelledUntilCurrentSegment += currentSegment.length

			val nextSegment = remainingSegments.head

			// instruct the vehicle to turn to the new segment
			vehicle ! Turn(currentSegment.calculateNecessaryTurn(nextSegment))

			currentSegment = nextSegment
			remainingSegments = remainingSegments.tail

			log.debug("RoadSegment ended, new segment length: " + currentSegment.length.formatted("%.2f"))
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
