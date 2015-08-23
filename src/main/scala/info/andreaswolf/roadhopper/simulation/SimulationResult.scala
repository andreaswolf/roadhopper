package info.andreaswolf.roadhopper.simulation

import scala.collection.mutable


class SimulationResult {

	val map = new mutable.HashMap[Int, VehicleState]()

	def setStatus(time: Int, status: VehicleState) = map.put(time, status)

	def toJsonObject = {
		map
	}

}
