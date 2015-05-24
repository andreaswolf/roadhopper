package info.andreaswolf.roadhopper.road

import junit.framework.Assert
import org.scalatest.FunSuite
import org.scalatest.prop.TableDrivenPropertyChecks._

class RoadSegmentTest extends FunSuite {

	test("Orientation is corrected if above pi") {
		var subject = new RoadSegment(200.0f, Math.PI * 2)
		Assert.assertEquals(0.0, subject.orientation)

		subject = new RoadSegment(200.0f, Math.PI * 3)
		Assert.assertEquals(Math.PI, subject.orientation)

		subject = new RoadSegment(200.0f, Math.PI * 2.5)
		Assert.assertEquals(Math.PI / 2, subject.orientation)
	}

	test("Orientation is corrected if below -pi") {
		var subject = new RoadSegment(200.0f, -Math.PI * 2)
		Assert.assertEquals(-0.0, subject.orientation)

		subject = new RoadSegment(200.0f, -Math.PI * 2.5)
		Assert.assertEquals(-Math.PI / 2, subject.orientation)

		subject = new RoadSegment(200.0f, -Math.PI * 3)
		Assert.assertEquals(Math.PI, subject.orientation)
	}

	test("Necessary turn is zero for same direction") {
		val segmentOne = new RoadSegment(100.0f, 0.0)
		val segmentTwo = new RoadSegment(200.0f, 0.0)

		Assert.assertEquals(0.0, segmentOne.calculateNecessaryTurn(segmentTwo))
	}

	val turns = Table(
		("fromOrientation", "toOrientation", "expectedTurn"),
		// right turns:
		(0.0, -Math.PI / 2, -Math.PI / 2),
		(Math.PI, -3 * Math.PI / 2, -Math.PI / 2),
		(Math.PI, 3 * Math.PI / 4, -Math.PI / 4),
		// left turns:
		(0.0, Math.PI / 2, Math.PI / 2),
		(Math.PI / 4, Math.PI / 2, Math.PI / 4),
		(Math.PI / 3, 2 * Math.PI / 3, Math.PI / 3)
	)
	forAll (turns) { (fromOrientation: Double, toOrientation: Double, expectedTurn: Double) =>
		test("Turn is correct for turn from " + fromOrientation + " to " + toOrientation) {
			val segmentOne = new RoadSegment(100.0f, fromOrientation)
			val segmentTwo = new RoadSegment(200.0f, toOrientation)

			Assert.assertEquals(expectedTurn, segmentOne.calculateNecessaryTurn(segmentTwo))
		}
	}

}
