package info.andreaswolf.roadhopper.simulation

import java.util

import info.andreaswolf.roadhopper.road.{RoadSegment, Route}
import org.slf4j.LoggerFactory

/**
 * The central class that coordinates a journey through a route
 */
class Simulator(val route: Route, val vehicle: Vehicle, val timestep: Int = 40) {

	val logger = LoggerFactory.getLogger(this.getClass)

	def simulate(): Journey = {
		// TODO replace this by proper initialization
		val initialStep = SimulationStep.initial(new VehicleState(1.0, 0))
		logger.info("Starting simulation")

		val steps = new util.LinkedList[SimulationStep]()
		steps.add(initialStep)
		var currentStep = initialStep

		val roadSegments: List[RoadSegment] = route.getRoadSegments
		var currentSegment = roadSegments.head
		while (currentStep.position < route.length) {
			currentStep = this.advance(currentStep, timestep)
			steps.add(currentStep)

			if (route.getSegmentForPosition(currentStep.position) != currentSegment) {
				logger.debug("Changed road segment at " + currentStep.time + " ms")
				currentSegment = route.getSegmentForPosition(currentStep.position)
			}
		}

		new Journey(steps)
	}

	/**
	 * Advance the simulation, based on lastState, by delta milliseconds
	 */
	protected def advance(lastState: SimulationStep, delta: Int): SimulationStep = {
		val time = lastState.time + delta

		val position = Math.min(route.length, lastState.position + lastState.vehicleState.speed * delta / 1000)
		val vehicleState = vehicle.calculateNewState(lastState.vehicleState, delta)
		new SimulationStep(time, position, vehicleState)
	}

}
