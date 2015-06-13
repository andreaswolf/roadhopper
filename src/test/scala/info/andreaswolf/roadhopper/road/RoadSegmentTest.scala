package info.andreaswolf.roadhopper.road

import com.graphhopper.util.shapes.GHPoint3D
import junit.framework.Assert
import org.scalatest.FunSuite
import org.scalatest.prop.TableDrivenPropertyChecks._

class RoadSegmentTest extends FunSuite {

	val orientationTest = Table(
		("initialOrientation", "expectedOrientation"),
		(Math.PI * 2, 0.0),
		(Math.PI * 3, -Math.PI),
		(Math.PI * 2.5, Math.PI / 2),
		(Math.PI * 1.5, -Math.PI / 2),
		(-Math.PI * 2, 0.0),
		(-Math.PI * 3, -Math.PI),
		(-Math.PI * 2.5, -Math.PI / 2),
		(-Math.PI * 1.5, Math.PI / 2)
	)
	forAll (orientationTest) { (initialOrientation: Double, expectedOrientation: Double) =>
		test("Orientation is corrected from " + initialOrientation + " to " + expectedOrientation) {
			val subject = new RoadSegment(new GHPoint3D(0.0, 0.0, 0.0), new GHPoint3D(0.1, 0.1, 0.0), 200.0f, initialOrientation)
			Assert.assertEquals(expectedOrientation, subject.orientation)
		}
	}

	test("Necessary turn is zero for same direction") {
		val segmentOne = new RoadSegment(new GHPoint3D(0.0, 0.0, 0.0), new GHPoint3D(0.1, 0.1, 0.0), 100.0f, 0.0)
		val segmentTwo = new RoadSegment(new GHPoint3D(0.0, 0.0, 0.0), new GHPoint3D(0.1, 0.1, 0.0), 200.0f, 0.0)

		Assert.assertEquals(0.0, segmentOne.calculateNecessaryTurn(segmentTwo))
	}

	val turns = Table(
		("fromOrientation", "toOrientation", "expectedTurn"),
		// right turns (turn < 0):
		// 0° -> -90°
		(0.0, -Math.PI / 2, -Math.PI / 2),
		// 180° -> -90°
		(-Math.PI, -3 * Math.PI / 2, -Math.PI / 2),
		(-Math.PI, Math.PI / 2, -Math.PI / 2),
		// 45° -> 0°
		(Math.PI / 4, 0.0, -Math.PI / 4),
		// left turns:
		(0.0, Math.PI / 2, Math.PI / 2),
		(Math.PI / 4, Math.PI / 2, Math.PI / 4),
		(Math.PI / 3, 2 * Math.PI / 3, Math.PI / 3)
	)
	forAll (turns) { (fromOrientation: Double, toOrientation: Double, expectedTurn: Double) =>
		test("Turn is correct for turn from " + fromOrientation + " to " + toOrientation) {
			val segmentOne = new RoadSegment(new GHPoint3D(0.0, 0.0, 0.0), new GHPoint3D(0.1, 0.1, 0.0), 100.0f, fromOrientation)
			val segmentTwo = new RoadSegment(new GHPoint3D(0.0, 0.0, 0.0), new GHPoint3D(0.1, 0.1, 0.0), 200.0f, toOrientation)

			Assert.assertEquals(expectedTurn, segmentOne.calculateNecessaryTurn(segmentTwo))
		}
	}

}
