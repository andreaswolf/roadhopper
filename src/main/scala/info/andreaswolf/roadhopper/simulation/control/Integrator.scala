/*
 * Copyright (c) 2015 Andreas Wolf
 *
 * See te LICENSE file in the project root for further copyright information.
 */

package info.andreaswolf.roadhopper.simulation.control



class Integrator(val gain: Double = 1.0) extends FirstOrderBlock {

	override def computeOutput(currentInput: Double, timeSpan: Int): Double = {
		currentState.currentOutput + gain * currentInput * timeSpan / 1000.0
	}

}
