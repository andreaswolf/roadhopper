package info.andreaswolf.roadhopper.road

import com.graphhopper.util.shapes.GHPoint3D
import junit.framework.Assert
import org.scalamock.scalatest.MockFactory

class RouteTest extends org.scalatest.FunSuite with MockFactory {

	test("An empty route should have length 0") {
		val list = List()
		Assert.assertEquals(0.0, new Route(list).length)
	}
/* TODO rework these tests to work again
	test("A route with two segments should have the length of both segments") {
		val firstSegment = mock[RoadSegment]
		(firstSegment.length _).when().returns(100.0)
		(firstSegment.orientation _).when().returns(0.0)
		val secondSegment = mock[RoadSegment]
		(secondSegment.length _).when().returns(50.0)
		(secondSegment.orientation _).when().returns(0.0)
		val list = List(
			firstSegment,
			secondSegment
		)
		Assert.assertEquals(150.0, new Route(list).length)
	}

	test("A segment in the route can be retrieved by position") {
		val list = List(
			new RoadSegment(new GHPoint3D(0.0,0.0,0.0), new GHPoint3D(0.1, 0.1, 0.0), 100.0f),
			new RoadSegment(new GHPoint3D(0.0,0.0,0.0), new GHPoint3D(0.1, 0.1, 0.0), 50.0f),
			new RoadSegment(new GHPoint3D(0.0,0.0,0.0), new GHPoint3D(0.1, 0.1, 0.0), 25.0f)
		)
		val route = new Route(list)

		Assert.assertTrue(route.getSegmentForPosition(50.0) == list.head)
		Assert.assertTrue(route.getSegmentForPosition(100.0) == list.head)
		Assert.assertTrue(route.getSegmentForPosition(100.1) == list.toArray.apply(1))
		Assert.assertTrue(route.getSegmentForPosition(125.0) == list.toArray.apply(1))
		Assert.assertTrue(route.getSegmentForPosition(150.0) == list.toArray.apply(1))
		Assert.assertTrue(route.getSegmentForPosition(150.1) == list.toArray.apply(2))
		Assert.assertTrue(route.getSegmentForPosition(175.0) == list.toArray.apply(2))
	}

	test("Accessing a route segment out of the bounds leads to an exception") {
		val list = List(
			new RoadSegment(new GHPoint3D(0.0,0.0,0.0), new GHPoint3D(0.1, 0.1, 0.0), 100.0f),
			new RoadSegment(new GHPoint3D(0.0,0.0,0.0), new GHPoint3D(0.1, 0.1, 0.0), 50.0f),
			new RoadSegment(new GHPoint3D(0.0,0.0,0.0), new GHPoint3D(0.1, 0.1, 0.0), 25.0f)
		)
		val route = new Route(list)

		intercept[IllegalArgumentException] {
			route.getSegmentForPosition(175.1)
		}
	}

	test("Coordinates for route points can be fetched") {
		val segments = List(new RoadSegment(new GHPoint3D(0.0,0.0,0.0), new GHPoint3D(0.1, 0.1, 0.0), 100.0f))
		val route = new Route(segments)

		Assert.assertEquals(List(new Tuple2(0.0, 0.0), new Tuple2(100.0f, 0.0)), route.roadSegmentCoordinates)
	}
*/

}
