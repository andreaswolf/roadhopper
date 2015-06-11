package info.andreaswolf.roadhopper.simulation

import akka.actor.{ActorRef, Props, Actor, ActorSystem}
import com.graphhopper.util.CmdArgs
import com.graphhopper.util.shapes.GHPoint
import info.andreaswolf.roadhopper.RoadHopper
import info.andreaswolf.roadhopper.road.{Route, RouteFactory}


object ActorBasedSimulation extends App {
	var timeBus = new TimedEventBus

	override def main(args: Array[String]): Unit = {
		val cmdArgs = CmdArgs.read(args)
		val roadHopperInstance = new RoadHopper
		roadHopperInstance.forServer().init(cmdArgs)
		roadHopperInstance.importOrLoad()

		val routeFactory = new RouteFactory(roadHopperInstance)
		val route = routeFactory.getRoute(List(new GHPoint(49.010796, 8.375444), new GHPoint(49.01271, 8.418016)))

		val simulation: ActorBasedSimulation = new ActorBasedSimulation(route)
		simulation.timer ! new Start
	}
}

class ActorBasedSimulation(val route: Route) {
	val actorSystem = ActorSystem.create("roadhopper")

	ActorBasedSimulation.timeBus = new TimedEventBus

	val timer = actorSystem.actorOf(Props[SimulationTimerActor], "timer")

	// TODO this should be moved to a separate method registerComponent()
	val vehicle = actorSystem.actorOf(Props(new VehicleActor(timer)), "vehicle")
	ActorBasedSimulation.timeBus.subscribe(vehicle, "time.step")

	val driver = actorSystem.actorOf(Props(new DriverActor(timer, vehicle)), "driver")
	ActorBasedSimulation.timeBus.subscribe(driver, "time.step")

	val monitor = actorSystem.actorOf(Props(new VehicleStatusMonitor(timer, 2000, vehicle)))
	ActorBasedSimulation.timeBus.subscribe(monitor, "time.step")

	timer ! StartSimulation(List(vehicle, driver, monitor))

	def shutdown() = actorSystem.shutdown()
}



