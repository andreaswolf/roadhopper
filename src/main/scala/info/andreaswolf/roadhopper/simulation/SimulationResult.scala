package info.andreaswolf.roadhopper.simulation

import info.andreaswolf.roadhopper.simulation.signals.SignalState

import scala.collection.mutable


/** The result of a simulation run */
class SimulationResult {

	/**
	 * The logged vehicle states. Using a LinkedHashMap to preserve insertion order.
	 * @deprecated Use the logged signals instead
	 */
	@deprecated
	val map: mutable.Map[Int, VehicleState] = new mutable.LinkedHashMap[Int, VehicleState]()

	/**
	 * All signal states, indexed by the time. Using a LinkedHashMap to preserve insertion order.
	 */
	val signals: mutable.Map[Int, SignalState] = new mutable.LinkedHashMap[Int, SignalState]()

	@deprecated
	def setStatus(time: Int, status: VehicleState) = map.put(time, status)

	/** Adds signal values for the given time step. */
	def setSignals(time: Int, signalState: SignalState) = signals.put(time, signalState)

	@deprecated
	def toJsonObject = {
		map
	}

}
