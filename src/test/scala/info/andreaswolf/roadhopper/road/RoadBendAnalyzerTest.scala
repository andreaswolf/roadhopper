/*
 * Copyright (c) 2015 Andreas Wolf
 *
 * See te LICENSE file in the project root for further copyright information.
 */

package info.andreaswolf.roadhopper.road

import com.graphhopper.util.shapes.GHPoint3D
import org.scalatest.FunSuite


class RoadBendAnalyzerTest extends FunSuite {

	test("Turn of 45° between two segments is recognized as a turn") {
		val road = new RoadBuilder(new GHPoint3D(49.0, 8.0, 0.0)).addSegment(100.0, 0.0).addSegment(100.0, 45.0).build

		val subject = new RoadBendAnalyzer
		val turns = subject.findTurns(road)

		assertResult(1)(turns.length)
		assertResult(road.apply(0))(turns.apply(0).from)
		assertResult(road.apply(1))(turns.apply(0).to)
	}

	test("Turn of -45° between two segments is recognized as a turn") {
		val road = new RoadBuilder(new GHPoint3D(49.0, 8.0, 0.0)).addSegment(100.0, 0.0).addSegment(100.0, -45.0).build

		val subject = new RoadBendAnalyzer
		val turns = subject.findTurns(road)

		assertResult(1)(turns.length)
		assertResult(road.apply(0))(turns.apply(0).from)
		assertResult(road.apply(1))(turns.apply(0).to)
	}

	test("Segment before is split into two segments") {
		val road = new RoadBuilder(new GHPoint3D(49.0, 8.0, 0.0)).addSegment(100.0, 0.0).addSegment(100.0, -45.0).build

		val subject = new RoadBendAnalyzer
		val segmentsWithTurns = subject.markTurns(road)

		assertResult(3)(segmentsWithTurns.length)
		assert(segmentsWithTurns.take(2).map(_.length).sum - road.head.length < 10e-3)
		assert(segmentsWithTurns.apply(1).isInstanceOf[PreTurnSegment])
	}

}
