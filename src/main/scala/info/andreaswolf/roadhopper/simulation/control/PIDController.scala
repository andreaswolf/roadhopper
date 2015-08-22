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
 * A controller consisting of a parallel proportional, integral and differential part.
 */
class PIDController(val inputSignalName: String, val outputSignalName: String,
                    val proportionalGain: Double, val integratorGain: Double, val differentiatorGain: Double,
                    val signalBus: ActorRef)
	extends Process(Some(signalBus)) with ActorLogging {

	import context.dispatcher


	val integrator = new Integrator(integratorGain)
	val differentiator = new Differentiator(differentiatorGain)


	val initialState = new ControllerState[Double](0.0, 0.0, 0)

	var currentState: ControllerState[Double] = null

	/**
	 * This value becomes the current state with the first invocation of [[timeAdvanced()]].
	 */
	var nextState: ControllerState[Double] = initialState


	override def timeAdvanced(oldTime: Int, newTime: Int): Future[Unit] = Future {
		currentState = nextState
		nextState = null
		integrator.timeAdvanced(oldTime, newTime)
		differentiator.timeAdvanced(oldTime, newTime)
	}

	override def invoke(signals: SignalState): Future[Any] = {
		val deltaT = time - currentState.time
		if (deltaT == 0) {
			return Future.successful()
		}
		val currentInput = signals.signalValue(inputSignalName, 0.0)

		integrator.update(currentInput)
		differentiator.update(currentInput)

		val output = (
			proportionalGain * currentInput
			+ integrator.currentState.currentOutput
			+ differentiator.currentState.currentOutput
		)

		nextState = new ControllerState[Double](output, currentInput, currentState, time)

		signalBus ? UpdateSignalValue(outputSignalName, output)
	}

}
