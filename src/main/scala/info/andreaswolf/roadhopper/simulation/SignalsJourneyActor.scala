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

	var currentRoadSet = false

	var travelledDistance = 0.0
	def currentPosition = currentSegmentRest.start

	var active = true


	registerReceiver({
		case GetRoadAhead(requestedLength) =>
			sender ! ReturnRoadAhead(getRoadAhead(requestedLength))
	})

	def updateRoad(): Future[Any] = {
		if (journeyEnded) {
			log.info("Journey ended after " + travelledUntilCurrentSegment + " (not accurate!)")
			// the shutdown will only be executed when all existing messages have been processed; therefore, we only tell the
			// timer to stop, but leave shutting down the system up to it
			timer ! Stop()
			return Future.successful()
		}
		// TODO dynamically calculate the distance to get (e.g. based on speed) or get it passed with the request
		// check if we have probably advanced past the current segment
		val oldSegment = currentSegment
		val segmentChanged = checkCurrentSegment()
		updateCurrentSegmentRest()

		val futures = ListBuffer[Future[Any]]()
		futures += signalBus ? UpdateSignalValue("pos", currentSegmentRest.start)

		// Update the segment-related signals. We must also do this for the first step, to get the velocity limit etc. into
		// the controller
		if (!currentRoadSet || segmentChanged) {
			currentRoadSet = true

			if (!(oldSegment isOnSameRoadAs currentSegment)) {
				log.info(s"Switched road from ${oldSegment.roadName.getOrElse("-unknown-")}" +
					s" to ${currentSegment.roadName.getOrElse("-unknown-")}")
				futures append signalBus ? UpdateSignalValue("road", currentSegment.roadName.getOrElse("-unknown-"))
			}

			futures append signalBus ? UpdateSignalValue("seg", currentSegment)
			futures append signalBus ? UpdateSignalValue("heading", currentSegment.orientation)
			futures append signalBus ? UpdateSignalValue("grade", currentSegment.grade)
			futures append signalBus ? UpdateSignalValue("v_limit", currentSegment.speedLimit)
		}

		Future.sequence(futures.toList)
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
				// there are more segments ahead, so just continue with the next one
		if (remainingOnCurrentSegment < 0.0 && remainingSegments.nonEmpty) {
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
			false
		}
	}

	/**
	 * Checks if
	 *
	 * a) we are on the last segment and
	 * b) if we travelled until after this segment’s end.
	 */
	def journeyEnded: Boolean = {
		remainingSegments.isEmpty && remainingOnCurrentSegment < 0.0
	}

	/**
	 * Returns the distance remaining on the current road segment, based on its length and the distance travelled
	 */
	def remainingOnCurrentSegment: Double = {
		travelledUntilCurrentSegment + currentSegment.length - travelledDistance
	}
}
