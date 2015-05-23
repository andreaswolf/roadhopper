package info.andreaswolf.roadhopper.road

import junit.framework.Assert

class RouteTest extends org.scalatest.FunSuite {

	test("An empty route should have length 0") {
		val list = List()
		Assert.assertEquals(0.0f, new Route(list).length)
	}

	test("A route with two segments should have the length of both segments") {
		val list = List(new RoadSegment(100.0f), new RoadSegment(50.0f));
		Assert.assertEquals(150.0f, new Route(list).length)
	}

}
