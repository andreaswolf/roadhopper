package info.andreaswolf.roadhopper

/**
 * This file is a playground to test new ideas for the simulation without the hassle of firing up a complete
 * GraphHopper instance etc.
 */

import akka.actor.{ActorRef, Props, ActorSystem, Actor}
import akka.pattern.ask
import akka.util.Timeout
import info.andreaswolf.roadhopper.simulation._

import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.duration._
import scala.util.{Failure, Success}

/**
 * This simulation coordinates the single steps with the help of futures, requiring every actor to explicitly respond
 * to a Step() message. Only after this response has been received from all actors, the current step is regarded as
 * being completed.
 */
object FutureBasedSimulation extends App {
	val actorSystem = ActorSystem.create("futures")

	val timer = actorSystem.actorOf(Props(new TwoStepSimulationTimer), "timer")

	val component = actorSystem.actorOf(Props(new Component(timer)), "component")
	val extensionComponent = actorSystem.actorOf(Props(new ExtensionComponent), "extension")

	implicit val ec: ExecutionContext = actorSystem.dispatcher
	implicit val timeout = Timeout(10 seconds)
	Future.sequence(List(
		timer ? RegisterActor(component),
		timer ? RegisterActor(extensionComponent)
	)) onSuccess {
		case x =>
			println("Starting")
			timer ! Simulate()
	}
}

case class Simulate()
case class StepUpdate(time: Int)
case class StepAct(time: Int)


class ExtensionComponent extends SimulationActor {

	override def stepUpdate(time: Int)(implicit exec: ExecutionContext): Future[Any] = Future {
		println("foo")
	}

	override def stepAct(time: Int)(implicit exec: ExecutionContext): Future[Any] = Future {
		println("bar")
	}
}

class AnotherComponent extends Actor {

	var foo = 0

	def receive = {
		case StepUpdate(time) =>
			println("AnotherComponent::StepUpdate")
			Thread.sleep(100)
			foo += 1
			sender ! true

		case StepAct(time) =>
			println("AnotherComponent::StepAct")
			Thread.sleep(150)
			println(f"foo: $foo")
			sender ! true

	}
}

class Component(val timer: ActorRef) extends Actor {

	implicit val timeout = Timeout(10 seconds)
	import context.dispatcher

	val subordinate = context.actorOf(Props(new Subordinate(self)), "subordinate")

	def receive = {
		case Start() =>
			val originalSender = sender()
			timer ? ScheduleRequest(50) andThen { case x =>
				println("Component::Start() finished")
				originalSender ! true
			}

		case StepUpdate(time) =>
			println("Component::StepUpdate")
			println("Sleeping 1000ms…")
			Thread.sleep(1000)
			println("1000ms done")
			println("Component: Updating time to " + time)
			sender ! true

		case StepAct(time) =>
			println("Component::StepAct")
			println("Sleeping 500…")
			Thread.sleep(500)
			println("500ms done")
			println("Component: Got time " + time)
			val originalSender = sender()
			Future sequence List(
				// ask another component a question => we need to directly handle the result inline; this could also be moved
				// to a method, but we cannot use this actor’s receive method, otherwise we cannot use sender() to send the
				// response in subordinate.
				subordinate ? Question(time) andThen {
					case Success(Answer(x)) =>
						println("Answer for " + time)
					case Success(x) => println(x)
				},
				timer ? ScheduleRequest(time + 100)
			) andThen {
				case x =>
					println(f"Scheduling request of ${self.path} passed")
					originalSender ! true
			}

		case Answer(time) =>
			println("Answer for " + time)
			sender ! true
	}
}

case class Question(time: Int)
case class Answer(time: Int)

class Subordinate(val component: ActorRef) extends Actor {
	implicit val timeout = Timeout(10 seconds)

	def receive = {
		case Question(time) =>
			println(f"Subordinate: $time")
			sender ! Answer(time)

	}
}
