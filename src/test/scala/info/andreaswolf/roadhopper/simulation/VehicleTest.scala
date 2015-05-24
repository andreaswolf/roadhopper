package info.andreaswolf.roadhopper.simulation

import junit.framework.Assert
import org.scalatest.FunSuite

/**
 * Created by aw on 23.05.15.
 */
class VehicleTest extends FunSuite {

	val subject = new Vehicle(3.0f, 20)

	test("Speed is kept constant without acceleration") {
		val oldState = createVehicleState(8, 0)

		Assert.assertEquals(oldState.speed, subject.calculateNewState(oldState, 100).speed)
	}

	test("Speed is increased for constant acceleration") {
		val oldState = createVehicleState(10, 1.0)

		Assert.assertEquals(11.0, subject.calculateNewState(oldState, 1000).speed)
	}

	test("Speed is changed accordingly in next step if acceleration decreases") {
		val firstState = createVehicleState(10, 1.0)
		val secondState = subject.calculateNewState(firstState, 1000)
	}

	test("Speed is not increased above maximum speed") {
		var state = createVehicleState(9.5, 3.0)
		for (i: Int <- 1 to 10) {
			state = subject.calculateNewState(state, 1000)
		}

		Assert.assertEquals(20.0, state.speed)
	}

	test("Acceleration is set to zero if maximum speed is reached") {
		val state = createVehicleState(19.5, 3.0)
		val newState = subject.calculateNewState(state, 1000)

		Assert.assertEquals(0.0, newState.acceleration)
	}


	private def createVehicleState(speed: Double, acceleration: Double): VehicleState = {
		new VehicleState(acceleration, speed, 0.0, driverInput = new DriverInput(0.0))
	}

}
