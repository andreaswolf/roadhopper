package info.andreaswolf.roadhopper.simulation

import akka.actor.{ActorRef, Props, Actor, ActorSystem}
import akka.event.LookupClassification
import akka.event.EventBus

import scala.collection.mutable


object ActorBasedSimulation extends App {
	var timeBus = new TimedEventBus

	override def main(args: Array[String]): Unit = {
		val simulation: ActorBasedSimulation = new ActorBasedSimulation()

		simulation.timer ! new Start
	}
}


class ActorBasedSimulation {
	val actorSystem = ActorSystem.create("roadhopper")

	ActorBasedSimulation.timeBus = new TimedEventBus

	val timer = actorSystem.actorOf(Props[SimulationTimerActor], "timer")

	// TODO this should be moved to a separate method registerComponent()
	val vehicle = actorSystem.actorOf(Props(new VehicleActor(timer)), "vehicle")
	ActorBasedSimulation.timeBus.subscribe(vehicle, "time.step")
	timer.tell(new ScheduleRequest(0), vehicle)

	val driver = actorSystem.actorOf(Props(new DriverActor(timer)), "driver")
	ActorBasedSimulation.timeBus.subscribe(driver, "time.step")
	timer.tell(new ScheduleRequest(0), driver)

	def shutdown() = actorSystem.shutdown()
}



