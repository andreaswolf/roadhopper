/*
 * Copyright (c) 2015 Andreas Wolf
 *
 * See te LICENSE file in the project root for further copyright information.
 */

package info.andreaswolf.roadhopper.simulation.control

import scala.concurrent.{ExecutionContext, Future}


/**
 * Generic implementation of a first-order block function. This class does the low-level handling of time and
 * state updates. To implement a specific function, extend it and implement [[computeOutput()]].
 */
abstract class FirstOrderBlock {

	val initialState = new ControllerState[Double](0.0, 0.0, 0)

	var currentState: ControllerState[Double] = null

	/**
	 * This value becomes the current state with the first invocation of [[timeAdvanced()]].
	 */
	var nextState: ControllerState[Double] = initialState

	var time = 0


	def timeAdvanced(oldTime: Int, newTime: Int)(implicit ec: ExecutionContext): Future[Unit] = Future {
		time = newTime
		currentState = nextState
		nextState = null
	}


	/**
	 * Updates this block’s internal state. The state update is only scheduled (written to [[nextState]]); it is moved
	 * to [[currentState]] when the next time step is reached (i.e. [[timeAdvanced()]] is called).
	 *
	 * @param currentInput The input value to compute
	 * @return True if the state was updated, false if no time has passed since the current state
	 */
	def update(currentInput: Double): Boolean = {
		val deltaT = time - currentState.time
		if (deltaT == 0) {
			return false
		}

		val output: Double = computeOutput(currentInput, deltaT)

		nextState = new ControllerState[Double](output, currentInput, currentState, time)
		true
	}


	/**
	 * Computes a new output value based on the function this class should implement.
	 *
	 * @return The new output value
	 */
	def computeOutput(currentInput: Double, timeSpan: Int): Double

}
