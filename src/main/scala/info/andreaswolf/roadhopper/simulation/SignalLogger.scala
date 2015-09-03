/*
 * Copyright (c) 2015 Andreas Wolf
 *
 * See te LICENSE file in the project root for further copyright information.
 */

package info.andreaswolf.roadhopper.simulation

import akka.actor.ActorRef
import info.andreaswolf.roadhopper.simulation.signals.{SignalState, Process}

import scala.collection.mutable
import scala.concurrent.Future


class SignalLogger(signalBus: ActorRef, val result: SimulationResult, val interval: Int = 250) extends Process(signalBus) {

	import context.dispatcher

	override def invoke(signals: SignalState): Future[Any] = Future {
		if (time % interval == 0) {
			result.setSignals(time, signals)
			println(s"Logged ${signals.values.size} signal values at $time")
		}
	}

}
