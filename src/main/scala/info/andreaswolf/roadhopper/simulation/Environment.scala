/*
 * Copyright (c) 2015 Andreas Wolf
 *
 * See te LICENSE file in the project root for further copyright information.
 */

package info.andreaswolf.roadhopper.simulation

import akka.actor.ActorRef
import akka.pattern.ask
import info.andreaswolf.roadhopper.simulation.signals.SignalBus.UpdateSignalValue

import scala.concurrent.{Future, ExecutionContext}


class Environment(val signalBus: ActorRef) extends SimulationActor {

	/**
	 * Handler for [[Start]] messages.
	 */
	override def start()(implicit exec: ExecutionContext): Future[Any] = {
		// fix the air pressure to 1000 hPa
		signalBus ? UpdateSignalValue("airPressure", 1000)
	}

}
