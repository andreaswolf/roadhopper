/*
 * Copyright (c) 2015 Andreas Wolf
 *
 * See te LICENSE file in the project root for further copyright information.
 */

package info.andreaswolf.roadhopper.simulation.control

import akka.actor.ActorRef
import akka.pattern.ask
import info.andreaswolf.roadhopper.simulation.signals.SignalBus.ScheduleSignalUpdate
import info.andreaswolf.roadhopper.simulation.signals.{SignalState, Process}

import scala.concurrent.Future


/**
 * A dead time controller that delays the input signal by the given time.
 *
 * @param delay The delay in milliseconds
 */
class DeadTime(inputSignalName: String, delay: Int, outputSignalName: String, bus: ActorRef) extends Process(bus) {

	override def invoke(signals: SignalState): Future[Any] = {
		bus ? ScheduleSignalUpdate(delay, outputSignalName, signals.signalValue(inputSignalName).getOrElse(0.0))
	}

}
