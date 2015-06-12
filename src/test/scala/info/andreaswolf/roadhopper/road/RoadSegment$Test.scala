package info.andreaswolf.roadhopper.road

import org.scalatest.FunSuite
import org.scalatest.prop.Tables.Table
import org.scalatest.prop.TableDrivenPropertyChecks._
import junit.framework.Assert

class RoadSegment$Test extends FunSuite {

	// calculated using <http://www.movable-type.co.uk/scripts/latlong.html>
	val coordinates = Table(
		("lat1", "lon1", "lat2", "lon2", "length", "orientation"),
		(  49.0,    8.0,   49.0,  8.001,    72.950, Math.PI / 2),
		(  49.0,  8.001,   49.0,  8.0  ,    72.950, -Math.PI / 2),
		(  49.0,    8.0, 49.001,    8.0,   111.200,   0.0),
		(49.001,    8.0,   49.0,    8.0,   111.200, Math.PI)
	)
	forAll(coordinates) {
		(x1: Double, y1: Double, x2: Double, y2: Double, expectedLength: Double, expectedOrientation: Double) =>
		test("line from (%.4f,%.4f) to (%.4f,%.4f)" format (x1,y1,x2,y2)) {
			val segment = RoadSegment.fromCoordinates(x1, y1, x2, y2)

			if (expectedLength < Double.MaxValue) {
				Assert.assertEquals(expectedLength, segment.length, 10e-3)
			}
			if (expectedOrientation < Double.MaxValue)
				Assert.assertEquals(expectedOrientation, segment.orientation, 10e-3)
		}
	}

}
