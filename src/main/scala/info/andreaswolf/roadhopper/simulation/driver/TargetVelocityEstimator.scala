/*
 * Copyright (c) 2015 Andreas Wolf
 *
 * See te LICENSE file in the project root for further copyright information.
 */

package info.andreaswolf.roadhopper.simulation.driver

import akka.actor.{ActorLogging, ActorRef}
import akka.pattern.ask
import info.andreaswolf.roadhopper.simulation.{ReturnRoadAhead, GetRoadAhead}
import info.andreaswolf.roadhopper.simulation.signals.SignalBus.UpdateSignalValue
import info.andreaswolf.roadhopper.simulation.signals.{SignalState, Process}

import scala.concurrent.Future


/**
 *
 */
class TargetVelocityEstimator(bus: ActorRef, journey: ActorRef) extends Process(bus) with ActorLogging {

	import context.dispatcher

	override def invoke(signals: SignalState): Future[Any] = {
		if (time % 500 > 0) {
			return Future.successful()
		}

		val currentVelocity = signals.signalValue("v", 0.0)
		// TODO pin the look ahead distance to the farthest point we ever encounter. i.e. save the value generated here
		// and always check a new value if it goes beyond this point; if not, skip it because it will not add value—the need
		// to lower our velocity will not vanish just because the new, lower velocity makes us look not so far ahead
		val lookAheadDistance: Int = (currentVelocity * currentVelocity / (2 * 4.0)).round.toInt
		journey ? GetRoadAhead(lookAheadDistance) flatMap {
			case ReturnRoadAhead(roadSegments) => Future {
				val minimumSpeedLimit = roadSegments.map(_.speedLimit).filter(_ > 0).min

				log.debug(f"Setting speed limit to $minimumSpeedLimit%.2f; looked ${lookAheadDistance}m ahead")
				bus ? UpdateSignalValue("v_target", minimumSpeedLimit)
			}
		}
	}
	
}
