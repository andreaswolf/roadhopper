package info.andreaswolf.roadhopper.simulation

import junit.framework.Assert
import org.scalatest.FunSuite

/**
 * Created by aw on 23.05.15.
 */
class VehicleTest extends FunSuite {

	val subject = new Vehicle(3.0f, 20)

	test("Speed is kept constant for no acceleration") {
		val oldState = new VehicleState(0, 8)

		Assert.assertEquals(oldState.speed, subject.calculateNewState(oldState, 100).speed)
	}

	test("Speed is increased for constant acceleration") {
		val oldState = new VehicleState(1.0, 10)

		Assert.assertEquals(11.0, subject.calculateNewState(oldState, 1000).speed)
	}

	test("Speed is changed accordingly in next step if acceleration decreases") {
		val firstState = new VehicleState(1.0, 10)
		val secondState = subject.calculateNewState(firstState, 1000)
	}

	test("Speed is not increased above maximum speed") {
		var state = new VehicleState(3.0, 9.5)
		for (i: Int <- 1 to 10) {
			state = subject.calculateNewState(state, 1000)
		}

		Assert.assertEquals(20.0, state.speed)
	}

	test("Acceleration is set to zero if maximum speed is reached") {
		val state = new VehicleState(3.0, 19.5)
		val newState = subject.calculateNewState(state, 1000)

		Assert.assertEquals(0.0, newState.acceleration)
	}

}
