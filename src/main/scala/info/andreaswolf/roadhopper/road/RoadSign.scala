package info.andreaswolf.roadhopper.road

import com.graphhopper.util.shapes.GHPoint

trait RoadSign {

	val coordinates: GHPoint

	val id: Int

	val typeInfo: String

}
