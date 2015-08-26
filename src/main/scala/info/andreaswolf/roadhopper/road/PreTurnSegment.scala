/*
 * Copyright (c) 2015 Andreas Wolf
 *
 * See te LICENSE file in the project root for further copyright information.
 */

package info.andreaswolf.roadhopper.road

import com.graphhopper.util.shapes.GHPoint3D


class PreTurnSegment(start: GHPoint3D, end: GHPoint3D, speedLimit: Double = 10 / 3.6)
	extends RoadSegment(start: GHPoint3D, end: GHPoint3D, speedLimit) {

}
