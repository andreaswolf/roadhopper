package info.andreaswolf.roadhopper.simulation

import com.graphhopper.util.shapes.GHPoint3D

object VehicleState {
	def getDefault() = new VehicleState(0.0, 0.0, 0.0)
}

/**
 *
 * @param acceleration The acceleration in meters per square second (m/(s^2^))
 * @param speed The speed in meters per second
 */
class VehicleState(val acceleration: Double, val speed: Double, val orientation: Double, val position: Option[GHPoint3D] = None) {

}
