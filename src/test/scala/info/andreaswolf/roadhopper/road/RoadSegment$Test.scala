package info.andreaswolf.roadhopper.road

import org.scalatest.FunSuite
import org.scalatest.prop.Tables.Table
import org.scalatest.prop.TableDrivenPropertyChecks._
import junit.framework.Assert

class RoadSegment$Test extends FunSuite {

	// lengths calculated using <http://www.movable-type.co.uk/scripts/latlong.html>
	val coordinates = Table(
		("lat1", "lon1", "lat2", "lon2", "length", "orientation"),
		(  49.0,    8.0,   49.0,  8.001,    72.950, Math.PI / 2),
		(  49.0,  8.001,   49.0,  8.0  ,    72.950, -Math.PI / 2),
		(  49.0,    8.0, 49.001,    8.0,   111.200,   0.0),
		(49.001,    8.0,   49.0,    8.0,   111.200, -Math.PI)
	)
	forAll(coordinates) {
		(lat1: Double, lon1: Double, lat2: Double, lon2: Double, expectedLength: Double, expectedOrientation: Double) =>
			test("line from (%.4f,%.4f) to (%.4f,%.4f)" format(lat1, lon1, lat2, lon2)) {
				val segment = RoadSegment.fromCoordinates(lat1, lon1, lat2, lon2)

				if (expectedLength < Double.MaxValue) {
					Assert.assertEquals(expectedLength, segment.length, 10e-3)
				}
				if (expectedOrientation < Double.MaxValue) {
					Assert.assertEquals(expectedOrientation, segment.orientation, 10e-3)
				}
			}
	}

	///////////////////////////////////////////////
	// Tests for fromExisting()
	///////////////////////////////////////////////

	test("new segment has same end point as base segment") {
		val base = RoadSegment.fromCoordinates(49.0, 8.0, 49.0, 8.001)

		val subject = RoadSegment.fromExisting(10, base)

		Assert.assertEquals(base.end, subject.end)
	}

	test("new segment keeps road sign") {
		val base = RoadSegment.fromCoordinates(49.0, 8.0, 49.0, 8.001)
		base.setRoadSign(Option(new StopSign(1, base.end)))

		val subject = RoadSegment.fromExisting(10, base)

		Assert.assertTrue(subject.roadSign.nonEmpty)
	}

	// lengths calculated using <http://www.movable-type.co.uk/scripts/latlong.html>
	val coordinatesForExisting = Table(
		("name", "lat1", "lon1", "lat2", "lon2"),
		("0.001° to east", 49.0, 8.0, 49.0, 8.001),
		("0.001° to west", 49.0, 8.001, 49.0, 8.0),
		("0.001° to north", 49.0, 8.0, 49.001, 8.0),
		("0.001° to south", 49.001, 8.0, 49.0, 8.0),
		("0.1° to north", 49.0, 8.0, 49.1, 8.0)
	)

	forAll(coordinatesForExisting) {
		(name: String, lat1: Double, lon1: Double, lat2: Double, lon2: Double) =>
			test(f"$name: new segment from end has correct length") {
				val base = RoadSegment.fromCoordinates(lat1, lon1, lat2, lon2)

				// these three use an offset from the end of the segment
				val tenMetersFromStart = RoadSegment.fromExisting(10, base)
				Assert.assertEquals(base.length - 10, tenMetersFromStart.length, 10e-3)

				val tenMetersBeforeEnd = RoadSegment.fromExisting(base.length - 10, base)
				Assert.assertEquals(10, tenMetersBeforeEnd.length, 10e-3)

				val centerFromEnd = RoadSegment.fromExisting(base.length / 2, base)
				Assert.assertEquals(base.length / 2, centerFromEnd.length, 10e-3)
			}

			test(f"$name: new segment from start has correct length") {
				val base = RoadSegment.fromCoordinates(lat1, lon1, lat2, lon2)

				// these two use an offset from the start of the segment
				val tenMetersFromEnd = RoadSegment.fromExisting(base, 10)
				Assert.assertEquals(base.length - 10, tenMetersFromEnd.length, 10e-3)

				val tenMetersAfterStart = RoadSegment.fromExisting(base, base.length - 10)
				Assert.assertEquals(10, tenMetersAfterStart.length, 10e-3)

				val centerFromStart = RoadSegment.fromExisting(base, base.length / 2)
				Assert.assertEquals(base.length / 2, centerFromStart.length, 10e-3)
			}
	}

}
