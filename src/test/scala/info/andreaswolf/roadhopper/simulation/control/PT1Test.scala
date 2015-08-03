/*
 * Copyright (c) 2015 Andreas Wolf
 *
 * See te LICENSE file in the project root for further copyright information.
 */

package info.andreaswolf.roadhopper.simulation.control

import akka.actor.ActorSystem
import akka.pattern.ask
import akka.testkit.{ImplicitSender, TestActorRef, TestKit, TestProbe}
import akka.util.Timeout
import info.andreaswolf.roadhopper.simulation.TellTime
import info.andreaswolf.roadhopper.simulation.signals.Process.Invoke
import info.andreaswolf.roadhopper.simulation.signals.SignalBus.UpdateSignalValue
import info.andreaswolf.roadhopper.simulation.signals.SignalState
import org.scalatest.{BeforeAndAfterAll, FunSuiteLike, Matchers}

import scala.concurrent.duration._


class PT1Test(_system: ActorSystem) extends TestKit(_system)
with FunSuiteLike with ImplicitSender with Matchers with BeforeAndAfterAll {

	implicit val timeout = Timeout(10 seconds)

	def this() = this(ActorSystem("ActorTest"))

	override def afterAll() {
		TestKit.shutdownActorSystem(system)
	}


	test("Step response is calculated correctly") {
		val signalBus = TestProbe()
		val subject = TestActorRef(new PT1("in", "out", 50, 1, bus = signalBus.ref))

		subject ? TellTime(10)
		subject ? Invoke(new SignalState(Map("in" -> 1.0)))
		signalBus.expectMsg(UpdateSignalValue("out", 0.16666666666666666))

		subject ? TellTime(20)
		subject ? Invoke(new SignalState(Map("in" -> 1.0)))
		signalBus.expectMsg(UpdateSignalValue("out", 0.3055555555555556))

		subject ? TellTime(30)
		subject ? Invoke(new SignalState(Map("in" -> 1.0)))
		signalBus.expectMsg(UpdateSignalValue("out", 0.4212962962962963))

		subject ? TellTime(40)
		subject ? Invoke(new SignalState(Map("in" -> 1.0)))
		signalBus.expectMsg(UpdateSignalValue("out", 0.5177469135802469))

		subject ? TellTime(50)
		subject ? Invoke(new SignalState(Map("in" -> 1.0)))
		signalBus.expectMsg(UpdateSignalValue("out", 0.5981224279835391))

		subject ? TellTime(60)
		subject ? Invoke(new SignalState(Map("in" -> 1.0)))
		signalBus.expectMsg(UpdateSignalValue("out", 0.6651020233196159))

		subject ? TellTime(70)
		subject ? Invoke(new SignalState(Map("in" -> 1.0)))
		signalBus.expectMsg(UpdateSignalValue("out", 0.7209183527663465))
	}

}
