package info.andreaswolf.roadhopper.simulation

import akka.actor._
import akka.pattern.ask
import akka.util.Timeout
import com.graphhopper.util.CmdArgs
import com.graphhopper.util.shapes.GHPoint
import info.andreaswolf.roadhopper.RoadHopper
import info.andreaswolf.roadhopper.road.{Route, RouteFactory}

import scala.collection.mutable.ListBuffer
import scala.concurrent.Await
import scala.concurrent.duration._


object ActorBasedSimulation extends App {
	override def main(args: Array[String]): Unit = {
		val cmdArgs = CmdArgs.read(args)
		val roadHopperInstance = new RoadHopper
		roadHopperInstance.forServer().init(cmdArgs)
		roadHopperInstance.importOrLoad()

		val routeFactory = new RouteFactory(roadHopperInstance)
		val points: List[GHPoint] = List(new GHPoint(49.010796, 8.375444), new GHPoint(49.01271, 8.418016))
		val route = routeFactory.simplify(routeFactory.getRoute(points).parts, 2.0)

		val simulation: ActorBasedSimulation = new ActorBasedSimulation(route)
		simulation.start()
	}
}

class ActorBasedSimulation(val route: Route) {
	val actorSystem = ActorSystem.create("roadhopper")

	val timer = actorSystem.actorOf(Props[TwoStepSimulationTimer], "timer")

	val actorBuffer = new ListBuffer[ActorRef]()

	val vehicle = registerActor(Props(new TwoStepVehicleActor(timer, route.getRoadSegments.head.orientation)), "vehicle")
	val journey = registerActor(Props(new TwoStepJourneyActor(timer, vehicle, route)), "journey")
	val driver = registerActor(Props(new TwoStepDriverActor(timer, vehicle, journey)), "driver")
	//val monitor = registerActor(Props(new VehicleStatusMonitor(timer, 2000, vehicle)), "monitor")

	implicit val timeout = Timeout(1 day)

	def start() = timer ! StartSimulation()

	def shutdown() = actorSystem.shutdown()

	def registerActor(actor: Props, name: String): ActorRef = {
		val actorRef = actorSystem.actorOf(actor, name)

		actorBuffer append actorRef
		implicit val timeout = Timeout(1 day)
		Await.result(timer ? RegisterActor(actorRef), 1 second)

		actorRef
	}
}



