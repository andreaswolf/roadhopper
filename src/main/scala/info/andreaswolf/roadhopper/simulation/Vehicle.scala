package info.andreaswolf.roadhopper.simulation

/**
 * Simple representation of a vehicle
 *
 * @param maxAcceleration The maximum acceleration in meters per square second (m/(s^2^))
 * @param maxSpeed The maximum speed in meters per second
 */
class Vehicle(val maxAcceleration: Float, val maxSpeed: Double) {

	def calculateNewState(currentState: VehicleState, delta: Int): VehicleState = {
		val speed = Math.min(maxSpeed, currentState.speed + (currentState.acceleration * delta / 1000))
		var acceleration = 0.0
		if (speed < maxSpeed) {
			acceleration = currentState.acceleration
		}

		new VehicleState(acceleration, speed, 0.0, driverInput = new DriverInput(0.0))
	}
}
