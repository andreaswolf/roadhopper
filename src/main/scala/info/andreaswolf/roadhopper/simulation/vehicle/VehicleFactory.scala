/*
 * Copyright (c) 2015 Andreas Wolf
 *
 * See te LICENSE file in the project root for further copyright information.
 */

package info.andreaswolf.roadhopper.simulation.vehicle

import akka.actor.{ActorRef, ActorRefFactory, Props}
import akka.pattern.ask
import akka.util.Timeout
import info.andreaswolf.roadhopper.simulation.control.DeadTime
import info.andreaswolf.roadhopper.simulation.signals.SignalBus
import info.andreaswolf.roadhopper.simulation.signals.SignalBus.{DefineSignal, SubscribeToSignal}

import scala.concurrent.duration._
import scala.concurrent.{Await, Future}


/**
 * A factory for vehicle actor subsystems.
 *
 * @param context The context the new actors should be created in
 */
class VehicleFactory(val context: ActorRefFactory, val timer: ActorRef, val signalBus: ActorRef) {

	implicit val timeout = Timeout(1 second)
	import context.dispatcher

	/**
	 *
	 */
	def createVehicle(parameters: VehicleParameters): ActorRef = {
		val throttle: ActorRef = context.actorOf(Props(new DeadTime("alpha", 50, "alpha*", signalBus)), "powerTrain-throttle")
		val engine: ActorRef = context.actorOf(Props(new Engine(vehicleParameters = parameters, signalBus = signalBus)), "engine")
		val wheels = context.actorOf(Props(new Wheels(vehicleParameters = parameters, signalBus)), "wheels")
		val powerTrainActor = context.actorOf(Props(new PowerTrain(throttle, engine, timer, signalBus)), "powerTrain")

		Await.result(Future.sequence(List(
			// The throttle input
			signalBus ? DefineSignal("alpha"),
			signalBus ? SubscribeToSignal("alpha", throttle),

			// The motor input (= throttle output)
			signalBus ? DefineSignal("alpha*"),
			signalBus ? SubscribeToSignal("alpha*", engine),
			signalBus ? SubscribeToSignal("time", engine),

			// The vehicle velocity/acceleration/travelled distance
			signalBus ? DefineSignal("v"),
			signalBus ? DefineSignal("a"),
			signalBus ? DefineSignal("s"),

			// The motor output (= torque); M is the direct motor output, M* is the wheel "input" that is delayed by the
			// inertia of the power train TODO check this again
			signalBus ? DefineSignal("M"),
			signalBus ? DefineSignal("M*"),
			signalBus ? SubscribeToSignal("M*", wheels),
			signalBus ? SubscribeToSignal("time", wheels)
			// TODO this currently leads to an endless loop
			//signalBus ? SubscribeToSignal("v", wheels)
		)), 1 second)

		context.actorOf(Props(new MotorizedVehicle(parameters, timer, signalBus, powerTrainActor)), "vehicle")
	}

}
