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
