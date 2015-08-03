/*
 * Copyright (c) 2015 Andreas Wolf
 *
 * See te LICENSE file in the project root for further copyright information.
 */

package info.andreaswolf.roadhopper.simulation

import akka.actor.{ActorSystem, Props}
import akka.pattern.ask
import akka.util.Timeout
import info.andreaswolf.roadhopper.simulation.signals.SignalBus
import info.andreaswolf.roadhopper.simulation.signals.SignalBus.ScheduleSignalUpdate
import info.andreaswolf.roadhopper.simulation.vehicle.{VehicleFactory, VehicleParameters}

import scala.concurrent.Future
import scala.concurrent.duration._


object SignalBasedSimulation extends App {
	val actorSystem = ActorSystem.create("futures")

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

	Future.sequence(List(
		timer ? RegisterActor(signalBus),
		timer ? RegisterActor(vehicle),
		signalBus ? ScheduleSignalUpdate(10, "alpha", 25)
	)) onSuccess {
		case x =>
			println("Starting")
			timer ! StartSimulation()
	}

}
