package info.andreaswolf.roadhopper.road

import com.graphhopper.util.shapes.GHPoint3D
import junit.framework.Assert
import org.scalatest.FunSuite
import org.scalatest.prop.TableDrivenPropertyChecks._
import org.yaml.snakeyaml.constructor.Tuple

class RoadSegmentTest extends FunSuite {

	test("Necessary turn is zero for same direction") {
		val segmentOne = new RoadSegment(new GHPoint3D(49.0, 8.0, 0.0), new GHPoint3D(49.0, 8.1, 0.0))
		val segmentTwo = new RoadSegment(new GHPoint3D(49.0, 8.1, 0.0), new GHPoint3D(49.0, 8.2, 0.0))

		Assert.assertEquals(0.0, segmentOne.calculateNecessaryTurn(segmentTwo))
	}

	val turnCoordinates = Table(
		// NOTE the distances need to be kept short, otherwise we get wrong results because of the
		// approximations used
		("name", "pointA", "pointB", "pointC", "expectedTurn"),
		("0° -> left 90°", (49.0, 8.0001), (49.0001, 8.0001), (49.0001, 8.0), -Math.PI / 2),
		("180° -> left 90°", (49.0001, 8.0), (49.0, 8.0), (49.0, 8.0001), -Math.PI / 2)
		// does not work ("0° -> left 45°", (49.0, 8.000001), (49.000001, 8.000001), (49.000002, 8.0), -Math.PI / 4)
	)
	forAll (turnCoordinates) { (name, pointA, pointB, pointC, expectedTurn) =>
		test("Turn " + name + " is correctly calculated") {
			val segmentOne = new RoadSegment(new GHPoint3D(pointA._1, pointA._2, 0.0), new GHPoint3D(pointB._1, pointB._2, 0.0))
			val segmentTwo = new RoadSegment(new GHPoint3D(pointB._1, pointB._2, 0.0), new GHPoint3D(pointC._1, pointC._2, 0.0))

			Assert.assertEquals(expectedTurn, segmentOne.calculateNecessaryTurn(segmentTwo), 1e-4)
		}
	}

	test("Segments are equal for same start and end") {
		val a: GHPoint3D = new GHPoint3D(49.0, 8.0, 0.0)
		val b: GHPoint3D = new GHPoint3D(49.01, 8.01, 0.0)
		assert(new RoadSegment(a, b) == new RoadSegment(a, b))
	}

	test("Segments are not equal if start and end are swapped") {
		val a: GHPoint3D = new GHPoint3D(49.0, 8.0, 0.0)
		val b: GHPoint3D = new GHPoint3D(49.01, 8.01, 0.0)
		assert(new RoadSegment(a, b) != new RoadSegment(b, a))
	}

	test("Default speed limit is 50 km/h") {
		val a: GHPoint3D = new GHPoint3D(49.0, 8.0, 0.0)
		val b: GHPoint3D = new GHPoint3D(49.01, 8.01, 0.0)
		val subject = new RoadSegment(a, b)

		assert(subject.speedLimit == 50 / 3.6)
	}

	test("Speed limit can be set in constructor") {
		val a: GHPoint3D = new GHPoint3D(49.0, 8.0, 0.0)
		val b: GHPoint3D = new GHPoint3D(49.01, 8.01, 0.0)
		val subject = new RoadSegment(a, b, speedLimit = 20.0)

		assert(subject.speedLimit == 20.0)
	}

}
