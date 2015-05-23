package info.andreaswolf.roadhopper.simulation

/**
 * Simple representation of a vehicle
 *
 * @param maxAcceleration The maximum acceleration in meters per square second (m/(s^2^))
 * @param maxSpeed The maximum speed in meters per second
 */
class Vehicle(val maxAcceleration: Float, val maxSpeed: Int) {

	def calculateNewState(currentState: VehicleState, delta: Int): VehicleState = {
		// TODO implement proper calculation
		new VehicleState(0, 10)
	}
}
