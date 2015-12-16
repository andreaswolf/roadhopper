/*
 * Copyright (c) 2015 Andreas Wolf
 *
 * See te LICENSE file in the project root for further copyright information.
 */

package info.andreaswolf.roadhopper.simulation.driver

import akka.actor.{ActorLogging, ActorRef}
import akka.pattern.ask
import info.andreaswolf.roadhopper.road.{RoadSignAnalyzer, StopSign, RoadSign}
import info.andreaswolf.roadhopper.simulation.signals.Process.Invoke
import info.andreaswolf.roadhopper.simulation.{TellTime, ReturnRoadAhead, GetRoadAhead}
import info.andreaswolf.roadhopper.simulation.signals.SignalBus.UpdateSignalValue
import info.andreaswolf.roadhopper.simulation.signals.{SignalState, Process}

import scala.concurrent.Future


/**
 * Component that derives the target velocity from the road’s current allowed velocity.
 *
 * It does only observe a part of the road within the so-called lookahead distance. This concept and the formula for
 * calculating the distance were derived from [[http://oops.uni-oldenburg.de/1762/1/Lenk_Jan_156.pdf this paper]].
 *
 * This component currently features two different modes: free driving and stop sign. The default is "free driving",
 * where only the limit from the road is relevant for the target speed.
 * If a stop sign is encountered within the lookahead distance, the current mode switches to "stop sign", where the
 * target velocity is reduced to zero. After the vehicle has stopped and a short time has passed, the vehicle is
 * speeded up again.
 */
class TargetVelocityEstimator(bus: ActorRef, journey: ActorRef) extends Process(bus) with ActorLogging {

	import context.dispatcher

	var farthestLookaheadPosition = 0
	/**
	 * The time the vehicle should start again after stopping at a stop sign
	 */
	var timeAfterStop = 0
	/**
	 * The target velocity set before switching to the stop-sign cycle
	 */
	var velocityBeforeStop = 0.0

	override def invoke(signals: SignalState): Future[Any] = {
		if (time % 500 > 0) {
			return Future.successful()
		}

		val currentPosition = signals.signalValue("s", 0.0)
		val currentVelocity = signals.signalValue("v", 0.0)
		// TODO pin the look ahead distance to the farthest point we ever encounter. i.e. save the value generated here
		// and always check a new value if it goes beyond this point; if not, skip it because it will not add value—the need
		// to lower our velocity will not vanish just because the new, lower velocity makes us look not so far ahead

		// The formula for calculating s_lookahead was taken from literature, but has not proven to be very good; this model
		// definitely needs improvement.
		val lookAheadDistance: Int = (currentVelocity * currentVelocity / (2 * 4.0)).round.toInt match {
			case x if currentPosition + x < farthestLookaheadPosition =>
				(farthestLookaheadPosition - currentPosition).ceil.toInt
			case x =>
				farthestLookaheadPosition = (currentPosition + x).ceil.toInt
				x
		}
		journey ? GetRoadAhead(lookAheadDistance) flatMap {
			case ReturnRoadAhead(roadSegments) => Future {
				val minimumSpeedLimit = roadSegments.map(_.speedLimit).filter(_ > 0).min

				// check if the look ahead distance contains any road signs which we must obey
				val roadSignsAhead: List[RoadSign] = roadSegments.flatMap(_.roadSign)
				if (roadSignsAhead.nonEmpty && roadSignsAhead.exists(_.isInstanceOf[StopSign])) {
					val distance = RoadSignAnalyzer.getDistanceUntilFirstSign(roadSegments, classOf[StopSign])

					// the 5m are used to ignore a stop sign we already stopped for and are now speeding away from
					// the sign might be behind the current look-ahead distance, as the segment it is on might be longer
					if (distance > 5.0 && distance < lookAheadDistance) {
						// TODO improve the stopping process to stop nearer than 5m. This requires more closely watching the current
						// speed and distance, or reducing the braking power; it is unclear if it is possible at all with the
						// current braking model to achieve such lower distances.
						log.info("Approaching a stop sign")
						context.become(approachingStopSign, discardOld = false)
					}
				}

				log.debug(f"Setting speed limit to $minimumSpeedLimit%.2f; looked ${lookAheadDistance}m ahead")
				bus ? UpdateSignalValue("v_target", minimumSpeedLimit)
			}
		}
	}

	def approachingStopSign: Receive = {
		case TellTime(_time) =>
			_advanceTime(_time)

		case Invoke(signalState) =>
			val originalSender = sender()
			invokeForStopSignal(signalState) andThen {
				case x =>
					originalSender ! true
			}
	}

	def invokeForStopSignal(signals: SignalState): Future[Any] = {
		if (timeAfterStop > 0 && time > timeAfterStop) {
			timeAfterStop = 0
			context unbecome()

			// just let the next cycle handle the future target speed
			bus ? UpdateSignalValue("v_target", velocityBeforeStop)
		} else if (timeAfterStop == 0) {
			velocityBeforeStop = signals.signalValue("v_target", 0.0)
			val currentVelocity = signals.signalValue("v", 0.0)

			if (currentVelocity < 1.0e-2) {
				timeAfterStop = time + 1000
			}

			bus ? UpdateSignalValue("v_target", 0.0)
		} else {
			Future.successful()
		}
	}

}
