/*
 * Copyright (c) 2015 Andreas Wolf
 *
 * See te LICENSE file in the project root for further copyright information.
 */

package info.andreaswolf.roadhopper.simulation

import java.lang.Long
import java.util.Date


/**
 * Base class for all simulations
 */
abstract class Simulation(val result: SimulationResult) {

	val identifier = Long.toHexString(new Date().getTime + ((Math.random() - 0.5) * 10e10).round)


	def start(): Unit

	def isFinished: Boolean

}
