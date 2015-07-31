/*
 * Copyright (c) 2015 Andreas Wolf
 *
 * See te LICENSE file in the project root for further copyright information.
 */

package info.andreaswolf.roadhopper.simulation.signals

import org.scalatest.FunSuite

import scala.collection.immutable.HashMap


class SignalStateTest extends FunSuite {

	test("Defined value can be fetched") {
		val subject = new SignalState(HashMap("foo" -> 1.0, "bar" -> 2))

		assert(subject.signalValue("foo").get == 1.0)
	}

	test("Values are correctly updated when using old map as base") {
		val oldMap = new SignalState(HashMap("foo" -> 1.0, "bar" -> 2))
		val subject = new SignalState(HashMap("foo" -> 3.0), oldMap)

		assert(subject.signalValue("foo").get == 3.0)
		assert(subject.signalValue("bar").get == 2)
	}

	test("Values are marked as updated when using old map as base") {
		val oldMap = new SignalState(HashMap("foo" -> 1.0, "bar" -> 2))
		val subject = new SignalState(HashMap("foo" -> 3.0), oldMap)

		assert(subject.isUpdated("foo"))
		assert(!subject.isUpdated("bar"))
	}

	test("getUpdated returns empty list by default") {
		val subject = new SignalState(HashMap("foo" -> 1.0, "bar" -> 2))

		assert(subject.getUpdated.isEmpty)
	}

	test("getUpdated returns correct map") {
		val oldMap = new SignalState(HashMap("foo" -> 1.0, "bar" -> 2))
		val updateMap: HashMap[String, Double] = HashMap("foo" -> 3.0)
		val subject = new SignalState(updateMap, oldMap)

		assert(subject.getUpdated == updateMap)
	}

}
