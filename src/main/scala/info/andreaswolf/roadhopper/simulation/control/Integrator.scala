/*
 * Copyright (c) 2015 Andreas Wolf
 *
 * See te LICENSE file in the project root for further copyright information.
 */

package info.andreaswolf.roadhopper.simulation.control

import akka.actor.{ActorLogging, ActorRef}
import akka.pattern.ask
import info.andreaswolf.roadhopper.simulation.signals.SignalBus.UpdateSignalValue
import info.andreaswolf.roadhopper.simulation.signals.{Process, SignalState}

import scala.concurrent.Future

/**
 * An integrator controller summarizes its input value over time.
 */
class Integrator(inputSignalName: String, outputSignalName: String, signalBus: ActorRef) extends Process(Some(signalBus))
with ActorLogging {

	import context.dispatcher

	var lastValue: Double = 0.0

	/**
	 * The last time this process was invoked *before* the current time. This value does not change during one time step,
	 * even if the process is invoked multiple times.
	 */
	var lastTimeStep = 0

	/**
	 * The time the process was last invoked. This value is updated with each update, that means it could be updated
	 * multiple times during one time step
	 */
	var lastInvocationTime = 0

	override def timeAdvanced(oldTime: Int, newTime: Int): Future[Unit] = Future {
		lastTimeStep = lastInvocationTime
	}

	/**
	 * The central routine of a process. This is invoked whenever a subscribed signal’s value changes.
	 */
	override def invoke(signals: SignalState): Future[Any] = {
		val deltaT = time - lastTimeStep
		if (deltaT == 0) {
			return Future.successful()
		}
		val currentInput = signals.signalValue(inputSignalName, 0.0)

		val newValue = lastValue + currentInput * deltaT / 1000.0

		lastInvocationTime = time
		lastValue = newValue

		signalBus ? UpdateSignalValue(outputSignalName, newValue)
	}
}
