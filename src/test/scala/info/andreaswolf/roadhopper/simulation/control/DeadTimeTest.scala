/*
 * Copyright (c) 2015 Andreas Wolf
 *
 * See te LICENSE file in the project root for further copyright information.
 */

package info.andreaswolf.roadhopper.simulation.control

import akka.actor.ActorSystem
import akka.pattern.ask
import akka.testkit.{ImplicitSender, TestKit, TestActorRef, TestProbe}
import akka.util.Timeout
import info.andreaswolf.roadhopper.simulation.TellTime
import info.andreaswolf.roadhopper.simulation.signals.Process.Invoke
import info.andreaswolf.roadhopper.simulation.signals.SignalBus.ScheduleSignalUpdate
import info.andreaswolf.roadhopper.simulation.signals.SignalState
import org.scalatest.{BeforeAndAfterAll, Matchers, FunSuiteLike}
import scala.concurrent.duration._


class DeadTimeTest(_system: ActorSystem) extends TestKit(_system)
with FunSuiteLike with ImplicitSender with Matchers with BeforeAndAfterAll {

	implicit val timeout = Timeout(10 seconds)

	def this() = this(ActorSystem("ActorTest"))

	override def afterAll() {
		TestKit.shutdownActorSystem(system)
	}

	test("Update for output value is scheduled after delay") {
		val signalBus = TestProbe()
		val subject = TestActorRef(new DeadTime("in", 10, "out", signalBus.ref))

		subject ? TellTime(10)
		subject ? Invoke(new SignalState(Map("in" -> 1.0)))
		signalBus.expectMsg(ScheduleSignalUpdate(10, "out", 1.0))
	}

}
