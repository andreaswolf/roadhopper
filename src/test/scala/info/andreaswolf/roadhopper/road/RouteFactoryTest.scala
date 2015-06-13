package info.andreaswolf.roadhopper.road

import info.andreaswolf.roadhopper.RoadHopper
import junit.framework.Assert
import org.scalamock.scalatest.MockFactory
import org.scalatest.{Matchers, FunSuite}

class RouteFactoryTest extends FunSuite with MockFactory with Matchers {

	test("Simplify merges two consecutive line segments") {
		val subject = new RouteFactory(mock[RoadHopper])

		val roadSegments = List(
			RoadSegment.fromCoordinates(49.0, 8.0, 49.1, 8.0),
			RoadSegment.fromCoordinates(49.1, 8.0, 49.2, 8.0)
		)

		val simplifiedRoute = subject.simplify(roadSegments)

		Assert.assertEquals(1, simplifiedRoute.parts.length)
		//("Merged segment has correct start") {
			Assert.assertEquals(49.0, simplifiedRoute.parts.head.asInstanceOf[RoadSegment].start.lat)
		//}
		//test("Merged segment has correct end") {
			Assert.assertEquals(49.2, simplifiedRoute.parts.head.asInstanceOf[RoadSegment].end.lat)
		//}
	}

	test("Simplify merges three consecutive line segments") {
		val subject = new RouteFactory(mock[RoadHopper])

		val roadSegments = List(
			RoadSegment.fromCoordinates(49.0, 8.0, 49.1, 8.0),
			RoadSegment.fromCoordinates(49.1, 8.0, 49.2, 8.0),
			RoadSegment.fromCoordinates(49.2, 8.0, 49.3, 8.0)
		)

		val simplifiedRoute = subject.simplify(roadSegments)

		Assert.assertEquals(1, simplifiedRoute.parts.length)
		Assert.assertEquals(49.0, simplifiedRoute.parts.head.asInstanceOf[RoadSegment].start.lat)
		Assert.assertEquals(49.3, simplifiedRoute.parts.head.asInstanceOf[RoadSegment].end.lat)
	}

	test("Simplify does not merge bended line segments") {
		val subject = new RouteFactory(mock[RoadHopper])

		val roadSegments = List(
			RoadSegment.fromCoordinates(49.0, 8.0, 49.1, 8.0),
			RoadSegment.fromCoordinates(49.1, 8.0, 49.1, 8.1),
			RoadSegment.fromCoordinates(49.1, 8.1, 49.1, 8.2)
		)

		val simplifiedRoute = subject.simplify(roadSegments)

		Assert.assertEquals(2, simplifiedRoute.parts.length)
		Assert.assertEquals(49.0, simplifiedRoute.parts.apply(0).asInstanceOf[RoadSegment].start.lat)
		Assert.assertEquals(49.1, simplifiedRoute.parts.apply(0).asInstanceOf[RoadSegment].end.lat)

		Assert.assertEquals(49.1, simplifiedRoute.parts.apply(1).asInstanceOf[RoadSegment].start.lat)
		Assert.assertEquals(8.0, simplifiedRoute.parts.apply(1).asInstanceOf[RoadSegment].start.lon)
		Assert.assertEquals(49.1, simplifiedRoute.parts.apply(1).asInstanceOf[RoadSegment].end.lat)
		Assert.assertEquals(8.2, simplifiedRoute.parts.apply(1).asInstanceOf[RoadSegment].end.lon)
	}

}
