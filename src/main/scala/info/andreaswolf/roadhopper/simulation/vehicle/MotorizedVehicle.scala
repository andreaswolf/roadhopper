/*
 * Copyright (c) 2015 Andreas Wolf
 *
 * See te LICENSE file in the project root for further copyright information.
 */

package info.andreaswolf.roadhopper.simulation.vehicle

import akka.actor.{ActorRef, Props}
import akka.pattern.ask
import info.andreaswolf.roadhopper.simulation.SimulationActor
import info.andreaswolf.roadhopper.simulation.signals.SignalBus.{SubscribeToSignal, UpdateSignalValue}
import info.andreaswolf.roadhopper.simulation.signals.{Process, SignalState}

import scala.concurrent.{Await, Future}
import scala.concurrent.duration._

/**
 * A Vehicle with a separate engine
 */
class MotorizedVehicle(val parameters: VehicleParameters, timer: ActorRef, signalBus: ActorRef,
                       powerTrain: ActorRef) extends SimulationActor {

	import context.dispatcher

	val velocityWatch = Props(new Process(Some(signalBus)) {
		var lastValue: Double = 0.0

		/**
		 * The last time this process was invoked *before* the current time. This value does not change during one time step,
		 * even if the process is invoked multiple times.
		 */
		var lastTimeStep = 0

		/**
		 * The time the process was last invoked. This value is updated with each update, that means it could be updated
		 * multiple times during one time step
		 */
		var lastInvocationTime = 0

		override def timeAdvanced(oldTime: Int, newTime: Int): Future[Unit] = Future {
			log.debug("VelocityWatch::timeAdvanced")
			lastTimeStep = lastInvocationTime
		}

		/**
		 * The central routine of a process. This is invoked whenever a subscribed signal’s value changes.
		 */
		override def invoke(signals: SignalState): Future[Any] = {
			log.debug("VelocityWatch::invoke")
			val deltaT = time - lastTimeStep
			if (deltaT == 0) {
				log.error("No time passed since last invocation: Cannot update signal")
				return Future.successful()
			}
			val currentInput = signals.signalValue("a", 0.0)

			val newVelocity = lastValue + currentInput * deltaT / 1000.0

			lastInvocationTime = time
			lastValue = newVelocity
			log.debug(s"Current velocity: $newVelocity")

			signalBus ? UpdateSignalValue("v", newVelocity)
		}
	})
	val distanceWatch = Props(new Process(Some(signalBus)) {
		var currentDistance: Double = 0.0

		var lastTimeStep = 0

		var lastInvocationTime = 0

		override def timeAdvanced(oldTime: Int, newTime: Int): Future[Unit] = Future {
			lastTimeStep = lastInvocationTime
		}

		override def invoke(signals: SignalState): Future[Any] = {
			val deltaT = time - lastTimeStep
			if (deltaT == 0) {
				log.error("No time passed since last invocation: Cannot update signal")
				return Future.successful()
			}
			val currentVelocity = signals.signalValue("v", 0.0)

			val newDistance = currentDistance + currentVelocity * deltaT / 1000.0

			lastInvocationTime = time
			currentDistance = newDistance
			log.debug("Travelled distance: " + currentDistance)

			signalBus ? UpdateSignalValue("s", newDistance)
		}
	})

	val velocityWatchActor = context.actorOf(velocityWatch, "velocityWatch")
	val distanceWatchActor = context.actorOf(distanceWatch, "distanceWatch")

	Await.result(Future.sequence(List(
		signalBus ? SubscribeToSignal("time", velocityWatchActor),
		signalBus ? SubscribeToSignal("time", distanceWatchActor)
	)), 1 second)

}
