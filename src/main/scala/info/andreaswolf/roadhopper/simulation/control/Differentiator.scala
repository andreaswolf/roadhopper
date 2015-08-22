/*
 * Copyright (c) 2015 Andreas Wolf
 *
 * See te LICENSE file in the project root for further copyright information.
 */

package info.andreaswolf.roadhopper.simulation.control


class Differentiator(val gain: Double = 1.0) extends FirstOrderBlock {

	override def computeOutput(currentInput: Double, timeSpan: Int): Double = {
		// "currentInput" in currentState refers to the last time’s input.
		gain * (currentInput - currentState.currentInput) / (timeSpan / 1000.0)
	}

}
