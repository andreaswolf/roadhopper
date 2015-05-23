package info.andreaswolf.roadhopper.simulation

import info.andreaswolf.roadhopper.road.{RoadSegment, Route}
import junit.framework.Assert
import org.scalatest.FunSuite

/**
 * Functional test for the simulator
 */
class SimulatorTest extends FunSuite {

	test("A journey should go until the end of the route") {
		val route = new Route(List(new RoadSegment(10)))
		val journey = new Simulator(route, new Vehicle(10,10)).simulate()

		Assert.assertEquals(10.0, journey.steps.get(journey.steps.size() - 1).position);
	}

}
