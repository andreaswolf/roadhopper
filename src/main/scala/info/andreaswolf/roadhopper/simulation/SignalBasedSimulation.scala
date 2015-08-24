/*
 * Copyright (c) 2015 Andreas Wolf
 *
 * See te LICENSE file in the project root for further copyright information.
 */

package info.andreaswolf.roadhopper.simulation

import java.lang.Long
import java.util.Date

import akka.actor.{ActorRef, ActorSystem, Props}
import akka.pattern.ask
import akka.util.Timeout
import com.graphhopper.util.CmdArgs
import com.graphhopper.util.shapes.GHPoint
import info.andreaswolf.roadhopper.RoadHopper
import info.andreaswolf.roadhopper.road.{Route, RouteFactory}
import info.andreaswolf.roadhopper.simulation.control.{PT1, PIDController}
import info.andreaswolf.roadhopper.simulation.driver.{TargetVelocityEstimator, VelocityController}
import info.andreaswolf.roadhopper.simulation.signals.{SignalState, SignalBus}
import info.andreaswolf.roadhopper.simulation.signals.SignalBus.{UpdateSignalValue, SubscribeToSignal, DefineSignal, ScheduleSignalUpdate}
import info.andreaswolf.roadhopper.simulation.vehicle.{VehicleFactory, VehicleParameters}

import scala.collection.mutable
import scala.concurrent.{Await, Future}
import scala.concurrent.duration._


object SignalBasedSimulation extends App {
	override def main(args: Array[String]): Unit = {
		val cmdArgs = CmdArgs.read(args)
		val roadHopperInstance = new RoadHopper
		roadHopperInstance.forServer().init(cmdArgs)
		roadHopperInstance.importOrLoad()

		val routeFactory = new RouteFactory(roadHopperInstance)
		val points: List[GHPoint] = List(new GHPoint(49.010796, 8.375444), new GHPoint(49.01271, 8.418016))
		val route = routeFactory.simplify(routeFactory.getRoute(points).parts, 2.0)

		val simulation: SignalBasedSimulation = new SignalBasedSimulation(route, new SimulationResult)
		simulation.start()
	}
}

class SignalBasedSimulation(val route: Route, override val result: SimulationResult) extends Simulation(result) {

	val actorSystem = ActorSystem.create("signals")

	import actorSystem.dispatcher
	implicit val timeout = Timeout(10 seconds)

	val timer = actorSystem.actorOf(Props(new TwoStepSimulationTimer), "timer")
	val signalBus = actorSystem.actorOf(Props(new SignalBus(timer)), "signalBus")

	val vehicleParameters = new VehicleParameters(
		mass = 1300,
		dragCoefficient = 0.29, dragReferenceArea = 2.4,
		wheelRadius = 32, wheelDragCoefficient = 0.012,
		maximumEnginePower = 84000, maximumEngineTorque = 200, maximumEngineRpm = 12000,
		engineEfficiencyFactor = 95,
		transmissionRatio = 10.0
	)

	val vehicle = new VehicleFactory(actorSystem, timer, signalBus).createVehicle(vehicleParameters)

	val velocityEstimator = actorSystem.actorOf(Props(new TargetVelocityEstimator(signalBus)))
	val journey = actorSystem.actorOf(Props(new SignalsJourneyActor(timer, signalBus, route)), "journey")
	val driver = actorSystem.actorOf(Props(new VelocityController(signalBus)))
	val velocityController = actorSystem.actorOf(Props(new PIDController("v_diff", "alpha_in", -0.0069, -2.59e-6, 5.35e-5, signalBus)))
	val gasPedal = actorSystem.actorOf(Props(new PT1("alpha_in", "alpha", 10, 500.0, 0.0, signalBus)))

	private val loggedSignals: mutable.Map[Int, SignalState] = new mutable.HashMap[Int, SignalState]()
	val signalLogger = actorSystem.actorOf(Props(new SignalLogger(signalBus, loggedSignals)))

	def start() = {
		Future.sequence(List(
			timer ? RegisterActor(signalBus),
			timer ? RegisterActor(vehicle),
			signalBus ? SubscribeToSignal("time", signalLogger),
			signalBus ? SubscribeToSignal("s", journey),
			signalBus ? SubscribeToSignal("time", velocityEstimator),
			signalBus ? SubscribeToSignal("time", velocityController),
			signalBus ? SubscribeToSignal("v_diff", velocityController),
			signalBus ? SubscribeToSignal("alpha_in", gasPedal)
		)) onSuccess {
			case x =>
				println("Starting")
				timer ! StartSimulation()
		}
	}

	def shutdown() = actorSystem.shutdown()

	def isFinished = actorSystem.isTerminated

	def registerActor(actor: Props, name: String): ActorRef = {
		val actorRef = actorSystem.actorOf(actor, name)

		implicit val timeout = Timeout(1 day)
		Await.result(timer ? RegisterActor(actorRef), 1 second)

		actorRef
	}

	def subscribeToSignal(signalName: String, actor: ActorRef) = {
		implicit val timeout = Timeout(1 day)
		Await.result(signalBus ? SubscribeToSignal(signalName, actor), 1 second)
	}
}
