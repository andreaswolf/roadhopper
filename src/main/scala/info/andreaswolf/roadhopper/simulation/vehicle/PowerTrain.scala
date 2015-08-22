/*
 * Copyright (c) 2015 Andreas Wolf
 *
 * See te LICENSE file in the project root for further copyright information.
 */

package info.andreaswolf.roadhopper.simulation.vehicle

import akka.actor.{Props, ActorLogging, ActorRef}
import akka.pattern.ask
import info.andreaswolf.roadhopper.simulation.SimulationActor
import info.andreaswolf.roadhopper.simulation.control.{DeadTime, PT1}
import info.andreaswolf.roadhopper.simulation.signals.SignalBus.{SubscribeToSignal, UpdateSignalValue}
import info.andreaswolf.roadhopper.simulation.signals.{SignalState, Process}

import scala.concurrent.{Await, Future}
import scala.concurrent.duration._

class PowerTrain(val throttle: ActorRef, val motor: ActorRef,
                 timer: ActorRef, signalBus: ActorRef) extends SimulationActor {

}


/**
 *
 */
class Engine(val vehicleParameters: VehicleParameters, signalBus: ActorRef) extends Process(signalBus) with ActorLogging {

	import context.dispatcher

	/**
	 * Calculate the engine force
	 */
	override def invoke(signals: SignalState): Future[Any] = {
		val loadFactor = signals.signalValue("alpha*", 0.0).round.min(100.0 toLong).max(0.0 toLong)
		val wheelAngularVelocity: Double =
			// make sure the vehicle is not rolling backwards; even if it is, the engine will only move it forward
			Math.max(0.0, signals.signalValue("v", 0.0)) / (2.0 * Math.PI * vehicleParameters.wheelRadius / 100.0)

		// the engine’s rotational speed in [1/s]; if the engine reaches the velocity limit, the rotation is set to
		// infinity to make the torque very small so the wheel/engine velocity does not exceed the limit (it might exceed
		// it by a few per mill. This is possible because the rotation is not exposed externally; if we ever expose it, we
		// must keep the actual rotation speed and the "helper" used to calculate the engine torque strictly apart.
		val rotation = wheelAngularVelocity match {
			case x if x == 0 => 0.00001
			case x if x * vehicleParameters.transmissionRatio > (vehicleParameters.maximumEngineRpm / 60) => Double.PositiveInfinity
			case x => wheelAngularVelocity * vehicleParameters.transmissionRatio
		}
		val M = Math.min(
			vehicleParameters.maximumEngineTorque,
			loadFactor / 100.0 * vehicleParameters.maximumEnginePower / (2.0 * Math.PI * rotation)
		)
		log.debug(s"loadFactor: $loadFactor, angular wheel velocity: $wheelAngularVelocity, rotation: $rotation, M: $M")

		signalBus ? UpdateSignalValue("M", M)
	}

	// The delay of the torque from the motor to the wheels
	val powerTrainInertia = context.actorOf(Props(new PT1("M", "M*", 100, bus = signalBus)))

	Await.result(Future.sequence(List(
		signalBus ? SubscribeToSignal("M", powerTrainInertia),
		signalBus ? SubscribeToSignal("time", powerTrainInertia)
	)), 1 second)


}


class Wheels(val vehicleParameters: VehicleParameters, bus: ActorRef) extends Process(bus) with ActorLogging {

	/**
	 * The central routine of a process. This is invoked whenever a subscribed signal’s value changes.
	 */
	override def invoke(signals: SignalState): Future[Any] = {
		// TODO respect vehicle’s current grade, also calculate the climbing resistance
		val rollingFrictionForce = vehicleParameters.wheelDragCoefficient * vehicleParameters.mass * 9.81

		val currentVelocity = signals.signalValue("v", 0.0)

		// TODO calculate this based on the current air pressure
		val airDensity: Double = 1.2
		// the "air resistance"
		val dragForce = (0.5 * airDensity
			* vehicleParameters.dragCoefficient * vehicleParameters.dragReferenceArea * currentVelocity * currentVelocity)

		val engineForce: Double = signals.signalValue("M", 0.0) * vehicleParameters.transmissionRatio / (vehicleParameters.wheelRadius / 100.0)

		log.debug(s"engine force: $engineForce, drag force: $dragForce, rolling friction force: $rollingFrictionForce")
		val effectiveForce = engineForce - rollingFrictionForce - dragForce

		// TODO add a factor for rotational inertia
		val acceleration = effectiveForce / vehicleParameters.mass

		bus ? UpdateSignalValue("a", acceleration)
	}

}
