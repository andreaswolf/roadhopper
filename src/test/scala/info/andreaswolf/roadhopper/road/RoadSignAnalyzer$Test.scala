/*
 * Copyright (c) 2015 Andreas Wolf
 *
 * See te LICENSE file in the project root for further copyright information.
 */

package info.andreaswolf.roadhopper.road

import com.graphhopper.util.shapes.GHPoint3D
import org.scalatest.FunSuite

class RoadSignAnalyzer$Test extends FunSuite {

	test("distance until current segment’s sign is segment length") {
		val segments = new RoadBuilder(new GHPoint3D(49.0, 8.0, 0.0)).addSegment(100, 0).addSegment(200, 90).build
		segments.head.setRoadSign(mockStopSign)

		val distance = RoadSignAnalyzer.getDistanceUntilFirstSign(segments, classOf[StopSign])

		assert(Math.abs(distance - 100.0) < 10e-3)
	}

	test("distance is zero if no sign is found") {
		val segments = new RoadBuilder(new GHPoint3D(49.0, 8.0, 0.0)).addSegment(100, 0).addSegment(200, 90).build

		val distance = RoadSignAnalyzer.getDistanceUntilFirstSign(segments, classOf[StopSign])

		assertResult(0.0)(distance)
	}

	test("distance until sign on second segment is combined length of segments") {
		val segments = new RoadBuilder(new GHPoint3D(49.0, 8.0, 0.0)).addSegment(100, 0).addSegment(200, 90).addSegment(100, 0).build
		segments.apply(1).setRoadSign(mockStopSign)

		val distance = RoadSignAnalyzer.getDistanceUntilFirstSign(segments, classOf[StopSign])

		assertResult(300.0)(distance)
	}

	def mockPosition = new GHPoint3D(1.0, 2.0, 3.0)

	def mockStopSign: StopSign = {
		new StopSign(123, mockPosition)
	}
}
