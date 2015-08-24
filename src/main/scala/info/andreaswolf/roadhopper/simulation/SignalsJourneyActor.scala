package info.andreaswolf.roadhopper.simulation

import akka.actor.{ActorLogging, ActorRef, Actor}
import akka.pattern.ask
import com.graphhopper.util.shapes.GHPoint3D
import info.andreaswolf.roadhopper.road.{RoadSegment, Route}
import info.andreaswolf.roadhopper.simulation.signals.SignalBus.UpdateSignalValue
import info.andreaswolf.roadhopper.simulation.signals.{SignalState, Process}

import scala.collection.mutable.ListBuffer
import scala.concurrent.{Future, ExecutionContext}


class SignalsJourneyActor(val timer: ActorRef, val signalBus: ActorRef, val route: Route)
	extends Process(signalBus) with ActorLogging {

	import context.dispatcher

	val length = route.parts.map(_.length).sum

	var remainingSegments = route.parts.tail
	var currentSegment = route.parts.head
	var travelledUntilCurrentSegment = 0.0

	var currentTime = 0

	var currentPosition: Option[GHPoint3D] = None

	var active = true

	def updateRoad(travelledDistance: Double): Future[Any] = {
		// TODO dynamically calculate the distance to get (e.g. based on speed) or get it passed with the request
		// check if we have probably advanced past the current segment
		checkCurrentSegment(travelledDistance)

		// make sure we only get segments after the current segment
		val remainingOnCurrentSegment = currentSegment.length - (travelledDistance - travelledUntilCurrentSegment)
		// if the length to get is 0, we will be on the current segment for all of the look-ahead distance
		var lengthToGet = Math.max(0, 150.0 - remainingOnCurrentSegment)

		val offsetOnCurrentSegment = travelledDistance - travelledUntilCurrentSegment

		val segmentsAhead = new ListBuffer[RoadSegment]
		// rare edge case: we travelled exactly to the end of the segment => we must skip it here
		if (remainingOnCurrentSegment > 0.0) {
			segmentsAhead append RoadSegment.fromExisting(offsetOnCurrentSegment, currentSegment)
		}
		remainingSegments.foreach(segment => {
			if (lengthToGet > 0) {
				segmentsAhead append segment
				lengthToGet -= segment.length
			}
		})
		currentPosition = Some(segmentsAhead.head.start)
		// if there are no more journey parts left after the current ones, this journey will end
		//val journeyEndsAfterFilteredSegments: Boolean = remainingSegments.length == segmentsAhead.length - 1

		//sender ! RoadAhead(currentTime, segmentsAhead.toList, journeyEndsAfterFilteredSegments)
		log.debug(f"Travelled until here: $travelledDistance, LengthToGet: $lengthToGet%.2f;" +
			f" got length: ${segmentsAhead.toList.map(_.length).sum}%.2f;" +
			f" segments: ${segmentsAhead.length - 1}/${remainingSegments.length}")

		Future.sequence(List(
			signalBus ? UpdateSignalValue("pos", segmentsAhead.head.start),
			signalBus ? UpdateSignalValue("seg", currentSegment)
		))
	}

	/**
	 * The central routine of a process. This is invoked whenever a subscribed signal’s value changes.
	 */
	override def invoke(signals: SignalState): Future[Any] = {
		if (!active) {
			return Future.successful()
		}

		val travelledDistance: Double = signals.signalValue[Double]("s", 0.0)
		val currentSpeed: Double = signals.signalValue[Double]("v", 0.0)

		if (currentSpeed == 0.0 && Math.abs(travelledDistance - length) < 1.5) {
			active = false
			timer ! Stop()
		}
		updateRoad(travelledDistance)
	}


	/**
	 * Checks if we are still on the current segment or if we moved beyond it and need to adjust the segment
	 * and the vehicle orientation
	 *
	 * @param position The current position on the journey, i.e. along the road to travel
	 * @return true if we are still within the road to travel, false if the journey has ended
	 */
	def checkCurrentSegment(position: Double): Boolean = {
		// are we at or beyond the current segment’s end?
		if (travelledUntilCurrentSegment + currentSegment.length - position > 10e-3) {
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
