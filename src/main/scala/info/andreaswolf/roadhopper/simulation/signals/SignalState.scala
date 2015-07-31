/*
 * Copyright (c) 2015 Andreas Wolf
 *
 * See te LICENSE file in the project root for further copyright information.
 */

package info.andreaswolf.roadhopper.simulation.signals

import scala.collection.{Map => BaseMap}
import scala.collection.immutable.{HashMap, Map => ImmutableMap}

object SignalState {

	type Values = ImmutableMap[String, AnyVal]

}

/**
 * A collection of signals and their values.
 */
class SignalState(val values: SignalState.Values, val updated: List[String] = List()) {

	type Values = ImmutableMap[String, AnyVal]

	def this(updatedValues: BaseMap[String, AnyVal], base: SignalState) = {
		this(base.values ++ updatedValues, updatedValues.keys.toList)
	}

	def this() = this(new HashMap[String, AnyVal]())

	/**
	 * Returns the given signal’s value, if any.
	 */
	def signalValue(name: String): Option[AnyVal] = values.get(name)

	def isUpdated(name: String) = updated.contains(name)

	/**
	 * Returns a partial map with all signals that were updated for this state
	 */
	def getUpdated: Values = values.filterKeys(name => updated.contains(name))

}
