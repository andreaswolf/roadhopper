/*
 * Copyright (c) 2015 Andreas Wolf
 *
 * See te LICENSE file in the project root for further copyright information.
 */

package info.andreaswolf.roadhopper.simulation.control

import org.scalatest.FunSuite

class ControllerStateTest extends FunSuite {

	test("New state can be constructed from old state and new values") {
		val oldState = new ControllerState[Double](0.0, 1.0, 0)
		val newState = new ControllerState[Double](0.5, 1.5, oldState, 10)

		assertResult(0.5)(newState.output.head)
		assertResult(0.5)(newState.currentOutput)
		assertResult(1.5)(newState.input.head)
		assertResult(1.5)(newState.currentInput)
	}

	test("Previous inputs and outputs can be fetched") {
		val oldState = new ControllerState[Double](0.0, 1.0, 0)
		val newState = new ControllerState[Double](0.5, 1.5, oldState, 10)

		assertResult(0.0)(newState.output.apply(1))
		assertResult(0.0)(newState.lastOutput)
		assertResult(1.0)(newState.input.apply(1))
		assertResult(1.0)(newState.lastInput)
	}

}
