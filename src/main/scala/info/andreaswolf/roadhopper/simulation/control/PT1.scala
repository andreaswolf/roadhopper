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
 * A PT1 proportional time-invariant controller.
 *
 * Its transfer function is "K / (1 + Ts)", with K being the amplification and T the time constant.
 *
 * @param inputSignalName The name of the input signal to listen to. Note that you manually need to register an instance
 *                        of this controller to listen to the signal.
 */
class PT1(inputSignalName: String, outputSignalName: String, timeConstant: Int, amplification: Double = 1.0,
          initialValue: Double = 0.0, bus: ActorRef) extends Process(Some(bus)) with ActorLogging {

	import context.dispatcher

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

	/**
	 * The last output value
	 */
	var lastOutput = 0.0


	override def timeAdvanced(oldTime: Int, newTime: Int): Future[Unit] = Future {
		lastTimeStep = lastInvocationTime
	}

	/**
	 * The central routine of a process. This is invoked whenever a subscribed signal’s value changes.
	 */
	override def invoke(signals: SignalState): Future[Any] = {
		// TODO we should have typesafe signals…
		val currentInput = signals.signalValue(inputSignalName).getOrElse(initialValue).asInstanceOf[Double]

		val deltaT = time - lastTimeStep
		if (deltaT == 0) {
			log.error("No time passed since last invocation: Cannot update signal")
			return Future.successful()
		}

		val timeFactor = 1.0 / (timeConstant / deltaT + 1.0)
		val newValue = timeFactor * (amplification * currentInput - lastOutput) + lastOutput
		lastOutput = newValue
		lastInvocationTime = time

		bus ? UpdateSignalValue(outputSignalName, newValue)
	}

}
