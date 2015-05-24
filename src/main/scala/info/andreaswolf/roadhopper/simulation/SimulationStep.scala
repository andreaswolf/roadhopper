package info.andreaswolf.roadhopper.simulation

object SimulationStep {
	def initial(vehicleState: VehicleState) = new SimulationStep(0, 0.0, vehicleState)
}

/**
 * 
 * @param time The point in time of this simulation step, in milliseconds (ms)
 */
class SimulationStep(val time: Int, val position: Double, var vehicleState: VehicleState) {

}
