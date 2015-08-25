/*
 * Copyright (c) 2015 Andreas Wolf
 *
 * See te LICENSE file in the project root for further copyright information.
 */

package info.andreaswolf.roadhopper.simulation.control

/**
 * The state of a controller, currently only keeping a value and the time.
 *
 * This is used to track when a controller was last invoked, to make sure updates always use the correct "old" value
 * (if multiple delta cycles for one time step update a controller value, the last update must win, and all must
 * use the correct old state (i.e. from before the current time step), not one from a previous delta cycle.
 */
class ControllerState[T](val output: Vector[T], val input: Vector[T], val time: Int) {

	def this(output: T, input: T, oldState: ControllerState[T], time: Int) {
		this(output +: oldState.output, input +: oldState.input, time)
	}

	def this(output: T, input: T, time: Int) {
		this(Vector(output), Vector(input), time)
	}


	def currentOutput = output.head

	def currentInput = input.head


	def lastOutput = output.apply(1)

	def lastInput = input.apply(1)


	def output(index: Int): T = output.apply(index)

	def input(index: Int): T = input.apply(index)
}
