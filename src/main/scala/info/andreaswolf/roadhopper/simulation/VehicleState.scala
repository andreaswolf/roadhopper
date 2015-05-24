package info.andreaswolf.roadhopper.simulation

object VehicleState {
	def getDefault() = new VehicleState(0.0, 0.0, 0.0, driverInput = new DriverInput(0.0))
}

/**
 *
 * @param acceleration The acceleration in meters per square second (m/(s^2^))
 * @param speed The speed in meters per second
 */
class VehicleState(val acceleration: Double, val speed: Double, val orientation: Double, val driverInput: DriverInput) {

}
