package info.andreaswolf.roadhopper.road

import com.graphhopper.util.shapes.GHPoint

trait RoadSign extends RoutePart {

	val coordinates: GHPoint

	val id: Int

}
