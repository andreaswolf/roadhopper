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

	test("Objects can be stored and fetched") {
		val data: Object = new Object()
		val subject = new SignalState(HashMap("foo" -> data))

		assert(subject.signalValue("foo").get == data)
	}

	test("Objects can be stored and fetched with type hint") {
		val data: Object = new Object()
		val subject = new SignalState(HashMap("foo" -> data))

		assertResult(data)(subject.signalValue[Object]("foo", new Object()))
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

	test("Default value is returned if no value is set") {
		val subject = new SignalState(HashMap("foo" -> 3.0))

		assert(subject.signalValue("bar", 1.0) == 1.0)
	}

	test("Getting value fails if expected return type does not match value type") {
		val subject = new SignalState(HashMap("foo" -> 3.0))

		intercept[ClassCastException] {
			subject.signalValue("foo", 2)
		}
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
