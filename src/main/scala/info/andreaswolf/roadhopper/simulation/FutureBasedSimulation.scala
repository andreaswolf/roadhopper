package info.andreaswolf.roadhopper.simulation

import akka.actor.{ActorSystem, Props}
import akka.pattern.ask
import akka.util.Timeout

import info.andreaswolf.roadhopper.{Simulate, ExtensionComponent, Component}

import scala.concurrent.Future
import scala.concurrent.ExecutionContext
import scala.concurrent.duration._

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

	implicit val timeout = Timeout(10 seconds)
	implicit val ec: ExecutionContext = actorSystem.dispatcher
	Future.sequence(List(
		timer ? RegisterActor(component),
		timer ? RegisterActor(extensionComponent)
	)) onSuccess {
		case x =>
			println("Starting")
			timer ! Simulate()
	}
}
