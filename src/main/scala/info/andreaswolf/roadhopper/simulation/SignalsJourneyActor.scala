package info.andreaswolf.roadhopper.simulation

import akka.actor.{ActorLogging, ActorRef}
import akka.pattern.ask
import info.andreaswolf.roadhopper.road.{RoadSegment, Route}
import info.andreaswolf.roadhopper.simulation.signals.SignalBus.UpdateSignalValue
import info.andreaswolf.roadhopper.simulation.signals.{Process, SignalState}

import scala.collection.mutable.ListBuffer
import scala.concurrent.Future


case class GetRoadAhead(length: Int)

// TODO rename this class once we got rid of RoadAhead in JourneyActor
case class ReturnRoadAhead(road: List[RoadSegment])


/**
 * This actor is responsible for tracking the progress of the vehicle along the route.
 * <p/>
 * It updates the position (signal "pos") and the current road segment ("seg") according to the travelled distance ("s").
 * It also tracks the remaining road segments and knows the vehicle’s orientation (according to the current road
 * segment).
 * <p/>
 * The actor can also be asked by other components to return a given length of the road ahead. See [[GetRoadAhead]] and
 * [[ReturnRoadAhead]] for more information.
 */
class SignalsJourneyActor(val timer: ActorRef, val signalBus: ActorRef, val route: Route)
	extends Process(signalBus) with ActorLogging {

	import context.dispatcher

	val length = route.parts.map(_.length).sum

	/**
	 * The segments remaining after the current segment
	 */
	var remainingSegments = route.parts.tail
	/**
	 * The full segment the vehicle is currently on
	 */
	var currentSegment = route.parts.head
	/**
	 * The remaining part of the segment the vehicle is currently on
	 */
	var currentSegmentRest = route.parts.head
	/**
	 * The distance that was travelled until the start of the current segment
	 */
	var travelledUntilCurrentSegment = 0.0

	var currentTime = 0

	var travelledDistance = 0.0
	def currentPosition = currentSegmentRest.start

	var active = true


	registerReceiver({
		case GetRoadAhead(requestedLength) =>
			sender ! ReturnRoadAhead(getRoadAhead(requestedLength))
	})

	def updateRoad(): Future[Any] = {
		// TODO dynamically calculate the distance to get (e.g. based on speed) or get it passed with the request
		// check if we have probably advanced past the current segment
		checkCurrentSegment()
		updateCurrentSegmentRest()

		Future.sequence(List(
			signalBus ? UpdateSignalValue("pos", currentSegmentRest.start),
			signalBus ? UpdateSignalValue("seg", currentSegment)
		))
	}

	def getRoadAhead(length: Double = 150.0): List[RoadSegment] = {
		// if the length to get is 0, we will be on the current segment for all of the look-ahead distance
		var lengthToGet = Math.max(0, length - currentSegmentRest.length)

		val segmentsAhead = new ListBuffer[RoadSegment]
		// rare edge case: we travelled exactly to the end of the segment => we must skip it here
		if (currentSegmentRest.length > 0.0) {
			segmentsAhead append currentSegmentRest
		}
		remainingSegments.foreach(segment => {
			if (lengthToGet > 0) {
				segmentsAhead append segment
				lengthToGet -= segment.length
			}
		})
		// if there are no more journey parts left after the current ones, this journey will end
		//val journeyEndsAfterFilteredSegments: Boolean = remainingSegments.length == segmentsAhead.length - 1

		//sender ! RoadAhead(currentTime, segmentsAhead.toList, journeyEndsAfterFilteredSegments)
		log.debug(f"Travelled until here: $travelledDistance, LengthToGet: $lengthToGet%.2f;" +
			f" got length: ${segmentsAhead.toList.map(_.length).sum}%.2f;" +
			f" segments: ${segmentsAhead.length - 1}/${remainingSegments.length}")

		segmentsAhead.toList
	}

	/**
	 * The central routine of a process. This is invoked whenever a subscribed signal’s value changes.
	 */
	override def invoke(signals: SignalState): Future[Any] = {
		if (!active) {
			return Future.successful()
		}

		travelledDistance = signals.signalValue[Double]("s", 0.0)
		val currentSpeed: Double = signals.signalValue[Double]("v", 0.0)

		if (currentSpeed == 0.0 && Math.abs(travelledDistance - length) < 1.5) {
			active = false
			timer ! Stop()
		}
		updateRoad()
	}

	def updateCurrentSegmentRest() = {
		val offsetOnCurrentSegment = travelledDistance - travelledUntilCurrentSegment

		currentSegmentRest = RoadSegment.fromExisting(offsetOnCurrentSegment, currentSegment)
	}

	/**
	 * Checks if we are still on the current segment or if we moved beyond it and need to adjust the segment
	 * and the vehicle orientation
	 *
	 * @return true if we are still within the road to travel, false if the journey has ended
	 */
	def checkCurrentSegment(): Boolean = {
		// are we at or beyond the current segment’s end?
		if (travelledUntilCurrentSegment + currentSegment.length - travelledDistance > 0.0) {
			return true
		}

		// there are more segments ahead, so just continue with the next one
		if (remainingSegments.nonEmpty) {
			travelledUntilCurrentSegment += currentSegment.length

			val nextSegment = remainingSegments.head

			// TODO implement this using signals
			// instruct the vehicle to turn to the new segment
			//vehicle ! Turn(currentSegment.calculateNecessaryTurn(nextSegment))

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
