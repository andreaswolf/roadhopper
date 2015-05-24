package info.andreaswolf.roadhopper.simulation

import java.util

import info.andreaswolf.roadhopper.road.Route
import org.slf4j.LoggerFactory

/**
 * The central class that coordinates a journey through a route
 */
class Simulator(val route: Route, val vehicle: Vehicle, val timestep: Int = 40) {

	val logger = LoggerFactory.getLogger(this.getClass)

	def simulate(): Journey = {
		val initialStep = new SimulationStep()

		var steps = new util.LinkedList[SimulationStep]()
		steps.add(initialStep)
		var currentStep = initialStep
		while (currentStep.position < route.length) {
			currentStep = this.advance(currentStep, timestep)
			steps.add(currentStep)
		}

		new Journey(steps)
	}

	/**
	 * Advance the simulation, based on lastState, by delta milliseconds
	 */
	protected def advance(lastState: SimulationStep, delta: Int): SimulationStep = {
		val time = lastState.time + delta
		logger.debug("Simulating time " + time)

		val position = Math.min(route.length, lastState.position + lastState.vehicleState.speed * delta / 1000)
		val vehicleState = vehicle.calculateNewState(lastState.vehicleState, delta)
		new SimulationStep(time, position, vehicleState)
	}

}
