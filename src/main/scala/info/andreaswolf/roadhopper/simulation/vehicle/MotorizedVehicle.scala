/*
 * Copyright (c) 2015 Andreas Wolf
 *
 * See te LICENSE file in the project root for further copyright information.
 */

package info.andreaswolf.roadhopper.simulation.vehicle

import akka.actor.{ActorRef, Props}
import akka.pattern.ask
import info.andreaswolf.roadhopper.simulation.SimulationActor
import info.andreaswolf.roadhopper.simulation.control.IController
import info.andreaswolf.roadhopper.simulation.signals.SignalBus.SubscribeToSignal

import scala.concurrent.duration._
import scala.concurrent.{Await, Future}

/**
 * A Vehicle with a completely modeled power train.
 *
 * This part of the model is responsible for tracking the directly externally visible vehicle parameters, i.e.
 * (currently) the speed and travelled distance.
 */
class MotorizedVehicle(val parameters: VehicleParameters, timer: ActorRef, signalBus: ActorRef,
                       powerTrain: ActorRef) extends SimulationActor {

	import context.dispatcher

	val velocityWatch = context.actorOf(Props(new IController("a", "v", signalBus)), "velocityWatch")
	val distanceWatch = context.actorOf(Props(new IController("v", "s", signalBus)), "distanceWatch")

	Await.result(Future.sequence(List(
		signalBus ? SubscribeToSignal("time", velocityWatch),
		signalBus ? SubscribeToSignal("time", distanceWatch)
	)), 1 second)

}
