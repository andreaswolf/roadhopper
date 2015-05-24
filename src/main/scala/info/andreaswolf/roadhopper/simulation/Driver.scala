package info.andreaswolf.roadhopper.simulation

/**
 * A vehicle driver, responsible for steering the vehicle.
 */
class Driver {

	def changeVehicleInput(currentStep: SimulationStep): DriverInput = {
		// TODO adjust steering wheel
		new DriverInput(0.0)
	}

}
