/*
 * Copyright (c) 2015 Andreas Wolf
 *
 * See te LICENSE file in the project root for further copyright information.
 */

package info.andreaswolf.roadhopper.simulation.vehicle

import akka.actor.{Props, ActorLogging, ActorRef}
import akka.pattern.ask
import info.andreaswolf.roadhopper.simulation.SimulationActor
import info.andreaswolf.roadhopper.simulation.control.{FirstOrderBlock, DeadTime, PT1}
import info.andreaswolf.roadhopper.simulation.signals.SignalBus.{SubscribeToSignal, UpdateSignalValue}
import info.andreaswolf.roadhopper.simulation.signals.{SignalBus, SignalState, Process}

import scala.concurrent.{Await, Future}
import scala.concurrent.duration._

class PowerTrain(val throttle: ActorRef, val motor: ActorRef,
                 timer: ActorRef, signalBus: ActorRef) extends SimulationActor {

}

class Brake(val inputSignalName: String, val outputSignalName: String, val delay: Int,
            signalBus: ActorRef) extends Process(signalBus) {

	import context.dispatcher

	val controlBlock = new FirstOrderBlock {
		/**
		 * Computes a new output value based on the function this class should implement.
		 *
		 * @return The new output value
		 */
		override def computeOutput(currentInput: Double, timeSpan: Int): Double = {
			currentInput.round.min(100).max(0)
		}
	}

	override def timeAdvanced(oldTime: Int, newTime: Int): Future[Unit] = Future {
		controlBlock.timeAdvanced(oldTime, newTime)
	}

	/**
	 * The central routine of a process. This is invoked whenever a subscribed signal’s value changes.
	 */
	override def invoke(signals: SignalState): Future[Any] = {
		if (controlBlock.update(signals.signalValue[Double](inputSignalName, 0.0))) {
			signalBus ? UpdateSignalValue(outputSignalName, controlBlock.nextState.currentOutput)
		} else {
			Future.successful()
		}
	}
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
		val loadFactor = signals.signalValue("alpha*", 0.0).round.min(100).max(0)
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
		val currentVelocity = signals.signalValue("v", 0.0)

		val grade: Double = signals.signalValue("grade", 0.0)
		val climbingResistance = currentVelocity match {
			case x if x > 0.0 => Math.sin(grade) * 9.81 * vehicleParameters.mass
			// no climbing resistance if the vehicle is not moving => avoid rolling backwards
			case x => 0.0
		}

		val rollingFrictionForce = currentVelocity match {
			case x if x > 0.0 =>
				// Some literature multiplies the drag with 4, but this is only necessary if the mass is calculated per
				// wheel, which we don’t have.
				vehicleParameters.wheelDragCoefficient * vehicleParameters.mass * 9.81 * Math.cos(grade)

			case x =>
				0.0
		}

		// TODO calculate this based on the current air pressure
		val airDensity: Double = 1.2
		// the "air resistance"
		val dragForce = (0.5 * airDensity
			* vehicleParameters.dragCoefficient * vehicleParameters.dragReferenceArea * currentVelocity * currentVelocity)

		val engineForce: Double = signals.signalValue("M", 0.0) * vehicleParameters.transmissionRatio / (vehicleParameters.wheelRadius / 100.0)

		val brakeForce = currentVelocity match {
			case x if x > 0.0 =>
				signals.signalValue("beta*", 0.0) * vehicleParameters.maximumBrakingForce
			case x =>
				0.0
		}

		val effectiveForce = engineForce - rollingFrictionForce - dragForce - brakeForce - climbingResistance
		log.info(s"forces: (eff/engine/drag/rolling/brake/climbing): $effectiveForce/$engineForce/$dragForce/$rollingFrictionForce/$brakeForce/$climbingResistance")

		// TODO add a factor for rotational inertia
		val acceleration = effectiveForce / vehicleParameters.mass

		bus ? UpdateSignalValue("a", acceleration)
	}

}
