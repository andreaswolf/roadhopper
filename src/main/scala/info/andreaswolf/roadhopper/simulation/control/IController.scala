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
 *
 * TODO implement a proportionality factor
 */
class IController(inputSignalName: String, outputSignalName: String, signalBus: ActorRef) extends Process(signalBus)
with ActorLogging {

	import context.dispatcher

	val _integrator = new Integrator()

	override def timeAdvanced(oldTime: Int, newTime: Int): Future[Unit] = Future {
		_integrator.timeAdvanced(oldTime, newTime)
	}

	/**
	 * The central routine of a process. This is invoked whenever a subscribed signal’s value changes.
	 */
	override def invoke(signals: SignalState): Future[Any] = {
		val currentInput = signals.signalValue(inputSignalName, 0.0)
		if (_integrator.update(currentInput)) {
			signalBus ? UpdateSignalValue(outputSignalName, _integrator.nextState.currentOutput)
		} else {
			Future.successful()
		}
	}
}
