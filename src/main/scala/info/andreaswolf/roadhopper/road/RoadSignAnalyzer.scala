/*
 * Copyright (c) 2015 Andreas Wolf
 *
 * See te LICENSE file in the project root for further copyright information.
 */

package info.andreaswolf.roadhopper.road

object RoadSignAnalyzer {

	def getDistanceUntilFirstSign(segments: List[RoadSegment], sign: Class[_ <: RoadSign]): Double = {
		val signs = segments.flatMap(_.roadSign)
		if (signs.exists(_.getClass == sign)) {
			val signIndex = segments.indexWhere(p => p.roadSign.isDefined && p.roadSign.get.getClass == sign)

			// +1 to also include the segment with the sign
			segments.take(signIndex + 1).map(_.length).sum
		} else {
			0.0
		}
	}

}
