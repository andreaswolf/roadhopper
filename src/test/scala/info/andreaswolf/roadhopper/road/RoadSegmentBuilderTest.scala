/*
 * Copyright (c) 2015 Andreas Wolf
 *
 * See te LICENSE file in the project root for further copyright information.
 */

package info.andreaswolf.roadhopper.road

import com.graphhopper.util.shapes.GHPoint3D
import org.scalatest.{Matchers, FunSuite}

class RoadSegmentBuilderTest extends FunSuite with Matchers {

	test("Construct segment from start and end") {
		val a: GHPoint3D = new GHPoint3D(49.0, 8.0, 0.0)
		val b: GHPoint3D = new GHPoint3D(49.01, 8.01, 0.0)

		val subject = new RoadSegmentBuilder
		subject.start(a)
		subject.end(b)
		val result = subject.build

		assert(result.isInstanceOf[RoadSegment])
		assert(result.start == a)
		assert(result.end == b)
	}

	test("Construct segment from start and end with fluent interface") {
		val a: GHPoint3D = new GHPoint3D(49.0, 8.0, 0.0)
		val b: GHPoint3D = new GHPoint3D(49.01, 8.01, 0.0)

		val subject = new RoadSegmentBuilder
		val result = subject.start(a).end(b).build

		assert(result.isInstanceOf[RoadSegment])
		assert(result.start == a)
		assert(result.end == b)
	}

	test("Constructed segment has speed limit if defined") {
		val a: GHPoint3D = new GHPoint3D(49.0, 8.0, 0.0)
		val b: GHPoint3D = new GHPoint3D(49.01, 8.01, 0.0)

		val subject = new RoadSegmentBuilder
		val result = subject.start(a).end(b).speedLimit(20).build

		assert(result.speedLimit == 20)
	}

}
