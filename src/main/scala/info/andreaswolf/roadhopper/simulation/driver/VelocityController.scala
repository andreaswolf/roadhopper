/*
 * Copyright (c) 2015 Andreas Wolf
 *
 * See te LICENSE file in the project root for further copyright information.
 */

package info.andreaswolf.roadhopper.simulation.driver

import akka.actor.{ActorLogging, ActorRef}
import akka.pattern.ask
import info.andreaswolf.roadhopper.simulation.signals.SignalBus.{DefineSignal, SubscribeToSignal, UpdateSignalValue}
import info.andreaswolf.roadhopper.simulation.signals.{Process, SignalState}

import scala.concurrent.{Await, Future}
import scala.concurrent.duration._


/**
 *
 */
class VelocityController(bus: ActorRef) extends Process(bus) with ActorLogging {

	import context.dispatcher

	Await.result(Future.sequence(List(
		bus ? SubscribeToSignal("v", self),
		bus ? SubscribeToSignal("v_target", self),
		bus ? DefineSignal("v_diff")
	)), 1 second)

	override def invoke(signals: SignalState): Future[Any] = {
		val actualVelocity: Double = signals.signalValue("v", 0.0)
		val targetVelocity: Double = signals.signalValue("v_target", 0.0)
		val velocityDifference = actualVelocity - targetVelocity

		log.debug(f"Velocity difference: $velocityDifference%.2f ($actualVelocity%.2f - $targetVelocity%.2f)")

		bus ? UpdateSignalValue("v_diff", velocityDifference)
	}

}
