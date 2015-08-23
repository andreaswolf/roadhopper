/*
 * Copyright (c) 2015 Andreas Wolf
 *
 * See te LICENSE file in the project root for further copyright information.
 */

package info.andreaswolf.roadhopper.simulation.signals

import scala.collection.{Map => BaseMap}
import scala.collection.immutable.{HashMap, Map => ImmutableMap}

object SignalState {

	type Values = ImmutableMap[String, Any]

}

/**
 * A collection of signals and their values.
 */
class SignalState(val values: SignalState.Values, val updated: List[String] = List()) {

	type Values = ImmutableMap[String, Any]

	def this(updatedValues: BaseMap[String, Any], base: SignalState) = {
		this(base.values ++ updatedValues, updatedValues.keys.toList)
	}

	def this() = this(new HashMap[String, Any]())

	/**
	 * Returns the given signal’s value, if any.
	 */
	def signalValue(name: String): Option[Any] = values.get(name)

	/**
	 * Returns the given signal’s value, or the default value if no value is set.
	 *
	 * @tparam T The type of the value to return. Note that this will cause an error if the value is not of the correct type.
	 * @return
	 */
	def signalValue[T](name: String, default: T): T = values.getOrElse(name, default).asInstanceOf[T]

	def isUpdated(name: String) = updated.contains(name)

	/**
	 * Returns a partial map with all signals that were updated for this state
	 */
	def getUpdated: Values = values.filterKeys(name => updated.contains(name))

}
