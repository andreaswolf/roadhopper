/*
 * Copyright (c) 2015 Andreas Wolf
 *
 * See te LICENSE file in the project root for further copyright information.
 */

package info.andreaswolf.roadhopper.simulation.signals

import akka.actor.{Props, ActorSystem}
import akka.pattern.ask
import akka.testkit.{TestProbe, TestActorRef, ImplicitSender, TestKit}
import akka.util.Timeout
import info.andreaswolf.roadhopper.simulation.{TellTime, StepUpdate}
import info.andreaswolf.roadhopper.simulation.signals.SignalBus._
import org.scalatest._

import scala.concurrent.{Await, Future}
import scala.concurrent.duration._


class SignalBusTest(_system: ActorSystem) extends TestKit(_system)
with FunSuiteLike with ImplicitSender with Matchers with BeforeAndAfterAll {

	implicit val timeout = Timeout(10 seconds)

	import system.dispatcher

	def this() = this(ActorSystem("ActorTest"))

	override def afterAll() {
		TestKit.shutdownActorSystem(system)
	}

	test("Signal cannot be registered twice") {
		val subject = TestActorRef(new SignalBus(new TestProbe(system).ref))

		val future = subject ? DefineSignal("testSignal")
		Await.result(future, 1 second)
		assert(future.value.get.get == true)

		val future2 = subject ? DefineSignal("testSignal")
		Await.result(future2, 1 second)
		assert(future2.value.get.get == "Signal already registered")
	}

	test("Updating a signal value triggers processes listening to that signal") {
		val subject = TestActorRef(new SignalBus(new TestProbe(system).ref))

		val testReceiver = TestProbe()

		val process = TestActorRef(new Process(subject) {
			override def invoke(state: SignalState): Future[Any] = Future {
				testReceiver.ref ! "invoked!"
			}
		})

		subject ? DefineSignal("test")
		subject ? SubscribeToSignal("test", process)
		// TODO this feels a bit weird because it is done outside the update cycle
		subject ? ScheduleSignalUpdate(10, "test", 1.0)
		subject ? TellTime(10)
		subject ? StepUpdate()

		testReceiver.expectMsg("invoked!")
	}

	test("Updated signal values are passed to the process") {
		val subject = TestActorRef(new SignalBus(new TestProbe(system).ref))

		val testReceiver = TestProbe()

		val process = TestActorRef(new Process(subject) {
			override def invoke(state: SignalState): Future[Any] = Future {
				testReceiver.ref ! state.signalValue("test").get
			}
		})

		subject ? DefineSignal("test")
		subject ? SubscribeToSignal("test", process)
		// TODO this feels a bit weird because it is done outside the update cycle
		subject ? UpdateSignalValue("test", 1.0)
		subject ? StepUpdate()

		testReceiver.expectMsg(1.0)
	}

	test("Updating another signal from a process triggers second delta cycle") {
		val subject = system.actorOf(Props(new SignalBus(new TestProbe(system).ref)))

		val testReceiver = TestProbe()

		val firstProcess = system.actorOf(Props(new Process(subject) {
			override def invoke(state: SignalState): Future[Any] = {
				testReceiver.ref ! "first invoked!"
				subject ? UpdateSignalValue("second", 2.0)
			}
		}))
		val secondProcess = system.actorOf(Props(new Process(subject) {
			override def invoke(state: SignalState): Future[Any] = Future {
				testReceiver.ref ! "second invoked!"
			}
		}))

		Await.result(Future.sequence(Vector(
			subject ? DefineSignal("test"),
			subject ? DefineSignal("second"),
			subject ? SubscribeToSignal("test", firstProcess),
			subject ? SubscribeToSignal("second", secondProcess),
			// TODO this feels a bit weird because it is done outside the update cycle
			subject ? ScheduleSignalUpdate(10, "test", 1.0)
		)), 10 seconds)
		Await.result(Future.sequence(Vector(
			subject ? TellTime(10)
		)), 10 seconds)

		// do not await the result of this future, as the timer will never respond to the schedule call in StepUpdate()
		subject ? StepUpdate()

		testReceiver.expectMsg("first invoked!")
		testReceiver.expectMsg("second invoked!")
		// TODO test here if there were really two delta cycles involved!
	}

	test("Subscriber for two changed signals is only called once") {
		val subject = TestActorRef(new SignalBus(new TestProbe(system).ref))

		val testReceiver = TestProbe()

		val process = TestActorRef(new Process(subject) {
			override def invoke(state: SignalState): Future[Any] = {
				testReceiver.ref ! "invoked!"
				Future.successful()
			}
		})

		Await.result(Future.sequence(Vector(
			subject ? DefineSignal("first"),
			subject ? DefineSignal("second"),
			subject ? SubscribeToSignal("first", process),
			subject ? SubscribeToSignal("second", process),
			subject ? ScheduleSignalUpdate(10, "first", 2.0),
			subject ? ScheduleSignalUpdate(10, "second", 3.0)
		)), 10 seconds)
		Await.result(Future.sequence(Vector(
				subject ? TellTime(10)
		)), 10 seconds)

		subject ? StepUpdate()

		testReceiver.expectMsg("invoked!")
		testReceiver.expectNoMsg()
	}

	test("Unchanged signal values are carried through to the next time slot") {
		val subject = TestActorRef(new SignalBus(new TestProbe(system).ref))

		val testReceiver = TestProbe()

		val process = TestActorRef(new Process(subject) {
			override def invoke(state: SignalState): Future[Any] = {
				testReceiver.ref ! state.signalValue("first").get
				Future.successful()
			}
		})
		subject ? DefineSignal("first")
		subject ? DefineSignal("second")
		subject ? SubscribeToSignal("first", process)
		subject ? SubscribeToSignal("second", process)

		subject ? UpdateSignalValue("first", 2.0)
		subject ? StepUpdate()
		subject ? UpdateSignalValue("second", 3.0)
		subject ? StepUpdate()

		testReceiver.expectMsg(2.0)
		testReceiver.expectMsg(2.0)
	}

	test("Time signal is triggered with each step") {
		val subject = TestActorRef(new SignalBus(new TestProbe(system).ref))

		val testReceiver = TestProbe()

		val process = TestActorRef(new Process(subject) {
			override def invoke(state: SignalState): Future[Any] = {
				testReceiver.ref ! time
				Future.successful()
			}
		})
		subject ? SubscribeToSignal("time", process)

		process ? TellTime(10)
		subject ? StepUpdate()
		testReceiver.expectMsg(10)

		process ? TellTime(20)
		subject ? StepUpdate()
		testReceiver.expectMsg(20)
	}

	test("Value updates scheduled for the future are executed before the first delta cycle") {
		val subject = TestActorRef(new SignalBus(new TestProbe(system).ref))

		val testReceiver = TestProbe()

		val process = TestActorRef(new Process(subject) {
			override def invoke(state: SignalState): Future[Any] = {
				testReceiver.ref ! state.signalValue("test").get
				Future.successful()
			}
		})
		subject ? DefineSignal("test")
		subject ? UpdateSignalValue("test", 1.0)
		subject ? SubscribeToSignal("test", process)

		subject ? TellTime(10)
		process ? TellTime(10)
		subject ? StepUpdate()
		testReceiver.expectMsg(1.0)
		subject ? ScheduleSignalUpdate(10, "test", 2.0)

		process ? TellTime(20)
		subject ? TellTime(20)
		subject ? StepUpdate()
		testReceiver.expectMsg(2.0)
	}

}
