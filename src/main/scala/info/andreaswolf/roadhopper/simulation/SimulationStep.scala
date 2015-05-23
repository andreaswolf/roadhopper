package info.andreaswolf.roadhopper.simulation

/**
 * 
 * @param time The point in time of this simulation step, in milliseconds (ms)
 */
class SimulationStep(val time: Int, val position: Double, var vehicleState: VehicleState) {

	/**
	 * Default constructor: returns the first step with a vehicle without any speed
	 *
	 * TODO should we move this (with a more fitting name) into a class object?
	 */
	def this() = this(0, 0, new VehicleState(0, 0))

}
