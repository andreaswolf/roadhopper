/*
 * Copyright (c) 2015 Andreas Wolf
 *
 * See te LICENSE file in the project root for further copyright information.
 */

package info.andreaswolf.roadhopper.simulation.driver

import akka.actor.{ActorLogging, ActorRef}
import akka.pattern.ask
import info.andreaswolf.roadhopper.simulation.signals.SignalBus.UpdateSignalValue
import info.andreaswolf.roadhopper.simulation.signals.{SignalState, Process}

import scala.concurrent.Future


/**
 *
 */
class TargetVelocityEstimator(bus: ActorRef) extends Process(bus) with ActorLogging {

	override def invoke(signals: SignalState): Future[Any] = {
		time match {
			case x if x == 10 =>
				// TODO get a real velocity here; for this, we need to define what constitutes the target velocity process
				bus ? UpdateSignalValue("v_target", 25.0)

			case x if x == 10000 =>
				bus ? UpdateSignalValue("v_target", 0.0)

			case x => Future.successful()
		}
	}
	
}
