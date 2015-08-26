package info.andreaswolf.roadhopper.simulation

import info.andreaswolf.roadhopper.simulation.signals.SignalState

import scala.collection.mutable


class SimulationResult {

	/**
	 * The logged vehicle states. Using a LinkedHashMap to preserve insertion order.
	 * @deprecated Use the logged signals instead
	 */
	val map: mutable.Map[Int, VehicleState] = new mutable.LinkedHashMap[Int, VehicleState]()

	/**
	 * All signal states. Using a LinkedHashMap to preserve insertion order.
	 */
	val signals: mutable.Map[Int, SignalState] = new mutable.LinkedHashMap[Int, SignalState]()

	def setStatus(time: Int, status: VehicleState) = map.put(time, status)

	def setSignals(time: Int, signalState: SignalState) = signals.put(time, signalState)

	def toJsonObject = {
		map
	}

}
