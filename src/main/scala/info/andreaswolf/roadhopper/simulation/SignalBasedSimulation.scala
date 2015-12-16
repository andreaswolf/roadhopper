/*
 * Copyright (c) 2015 Andreas Wolf
 *
 * See te LICENSE file in the project root for further copyright information.
 */

package info.andreaswolf.roadhopper.simulation

import akka.actor.{ActorRef, ActorSystem, Props}
import akka.pattern.ask
import akka.util.Timeout
import com.graphhopper.util.CmdArgs
import com.graphhopper.util.shapes.GHPoint
import info.andreaswolf.roadhopper.RoadHopper
import info.andreaswolf.roadhopper.road.{Route, RouteFactory}
import info.andreaswolf.roadhopper.simulation.SimulationParameters.PedalParameters
import info.andreaswolf.roadhopper.simulation.control.{PIDController, PT1}
import info.andreaswolf.roadhopper.simulation.driver.{TargetVelocityEstimator, VelocityController}
import info.andreaswolf.roadhopper.simulation.signals.SignalBus.SubscribeToSignal
import info.andreaswolf.roadhopper.simulation.signals.{SignalBus, SignalState}
import info.andreaswolf.roadhopper.simulation.vehicle.{Brake, VehicleFactory, VehicleParameters}

import scala.collection.mutable
import scala.concurrent.duration._
import scala.concurrent.{Await, Future}


object SignalBasedSimulation extends App {
	/**
	 * Standalone app for testing the signal-based routing. To use RoadHopper in an actual use case, start
	 * [[info.andreaswolf.roadhopper.server.RoadHopperServer]] instead.
	 */
	override def main(args: Array[String]): Unit = {
		val cmdArgs = CmdArgs.read(args)
		val roadHopperInstance = new RoadHopper
		roadHopperInstance.forServer().init(cmdArgs)
		roadHopperInstance.importOrLoad()

		val routeFactory = new RouteFactory(roadHopperInstance)
		val points: List[GHPoint] = List(new GHPoint(49.010796, 8.375444), new GHPoint(49.01271, 8.418016))
		val route = routeFactory.simplify(routeFactory.getRoute(points).parts, 2.0)

		val simulation: SignalBasedSimulation = new SignalBasedSimulation(new SimulationParameters(
			pedal = new PedalParameters(gasPedalGain = 100.0, brakePedalGain = -200.0),
			vehicle = VehicleParameters.CompactCar,
			route = route
		), new SimulationResult)
		simulation.start()
	}
}

/**
 * Main class of the signal-based simulator.
 *
 * @param simulationParameters The parameters to apply (brake/gas pedal gains, vehicle etc.)
 * @param result Where the simulation results should be stored
 */
class SignalBasedSimulation(val simulationParameters: SimulationParameters, override val result: SimulationResult)
	extends Simulation(result) {

	/**
	 * Compatibility constructor to instantiate the simulation with just a route. A default set of parameters will be
	 * used
	 */
	def this(route: Route, result: SimulationResult) {
		this(new SimulationParameters(
			pedal = new PedalParameters(gasPedalGain = 500.0, brakePedalGain = -500.0),
			vehicle = VehicleParameters.CompactCar,
			route = route
		), result)
	}

	/** The route that is travelled during the simulation */
	val route = simulationParameters.route

	val actorSystem = ActorSystem.create("signals")

	import actorSystem.dispatcher
	implicit val timeout = Timeout(10 seconds)

	/** The two base components of the simulation: the timer and the signal bus;
	 * in the future, there might be multiple signal busses, e.g. to simulate multiple vehicles or partition the vehicle
	 * if the simulation gets more fine-grained. */
	val timer = actorSystem.actorOf(Props(new TwoStepSimulationTimer), "timer")
	val signalBus = actorSystem.actorOf(Props(new SignalBus(timer)), "signalBus")

	/** The vehicle used in the simulation */
	val vehicle = new VehicleFactory(actorSystem, timer, signalBus).createVehicle(simulationParameters.vehicle)

	val journey = actorSystem.actorOf(Props(new SignalsJourneyActor(timer, signalBus, simulationParameters.route)), "journey")

	///////////////////////////////////////////////////////////////////////////
	// Components of the driver model
	///////////////////////////////////////////////////////////////////////////
	val velocityEstimator = actorSystem.actorOf(Props(new TargetVelocityEstimator(signalBus, journey)))
	val targetVelocityCalculator = actorSystem.actorOf(Props(new VelocityController(signalBus)))
	val velocityController = actorSystem.actorOf(Props(
		new PIDController("v_diff", "alpha_in",
			simulationParameters.velocityController.proportionalGain, simulationParameters.velocityController.integratorGain,
			simulationParameters.velocityController.differentiatorGain, signalBus)
	))

	///////////////////////////////////////////////////////////////////////////
	// Pedals and brake system
	//
	// The gas pedal connects to the transmission which was created by the
	// VehicleFactory instance above
	///////////////////////////////////////////////////////////////////////////
	val gasPedal = actorSystem.actorOf(Props(new PT1("alpha_in", "alpha", 100, simulationParameters.pedal.gasPedalGain, 0.0, signalBus)))
	val brakePedal = actorSystem.actorOf(Props(new PT1("alpha_in", "beta", 100, simulationParameters.pedal.brakePedalGain, 0.0, signalBus)))
	val brake = actorSystem.actorOf(Props(new Brake("beta", "beta*", 100, signalBus)))

	/** The result logger for the simulation run. */
	val signalLogger = actorSystem.actorOf(Props(new SignalLogger(signalBus, result, 50)))

	/** Invoked to start the simulation by sending the required signal to the timer */
	def start() = {
		Future.sequence(List(
			timer ? RegisterActor(signalBus),
			timer ? RegisterActor(vehicle),
			// subscribe all components above to their respective signals; some components also do this on their own
			// TODO clean up once signal registration and subscription have been streamlined
			signalBus ? SubscribeToSignal("time", signalLogger),
			signalBus ? SubscribeToSignal("s", journey),
			signalBus ? SubscribeToSignal("s", velocityEstimator),
			signalBus ? SubscribeToSignal("time", velocityController),
			signalBus ? SubscribeToSignal("v_diff", velocityController),
			signalBus ? SubscribeToSignal("alpha_in", gasPedal),
			signalBus ? SubscribeToSignal("alpha_in", brakePedal),
			signalBus ? SubscribeToSignal("beta", brake)
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
