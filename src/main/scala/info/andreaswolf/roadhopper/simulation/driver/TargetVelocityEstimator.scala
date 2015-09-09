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
 *
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

				val roadSignsAhead: List[RoadSign] = roadSegments.flatMap(_.roadSign)
				if (roadSignsAhead.nonEmpty && roadSignsAhead.exists(_.isInstanceOf[StopSign])) {
					val distance = RoadSignAnalyzer.getDistanceUntilFirstSign(roadSegments, classOf[StopSign])

					// the sign might be behind the current look-ahead distance, as the segment it is on might be longer
					// ignore short distances to the sign to not get trapped in an endless loop
					// the 5m are used
					// TODO improve the stopping process to stop nearer than 5m. This requires more closely watching the current
					// speed and distance, or reducing the braking power
					if (distance > 5.0 && distance < lookAheadDistance) {
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
