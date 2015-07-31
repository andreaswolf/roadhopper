/*
 * Copyright (c) 2015 Andreas Wolf
 *
 * See te LICENSE file in the project root for further copyright information.
 */

package info.andreaswolf.roadhopper.simulation.signals

import akka.actor.Actor
import info.andreaswolf.roadhopper.simulation.TellTime
import info.andreaswolf.roadhopper.simulation.signals.Process._

import scala.concurrent.Future

object Process {

	case class Invoke(signalState: SignalState)

}

/**
 * A process is a component that uses one or more signals for calculations.
 *
 * The process subscribes to the signals it needs for its calculations and is notified by the signal bus each time a
 * signal changes.
 */
abstract class Process extends Actor {

	import context.dispatcher

	var time: Int = 0

	def receive = {
		case TellTime(_time) =>
			time = _time
			sender() ! true

		case Invoke(signalState) =>
			val originalSender = sender()
			invoke(signalState) andThen {
				case x =>
					originalSender ! true
			}

	}

	/**
	 * The central routine of a process. This is invoked whenever a subscribed signal’s value changes.
	 */
	def invoke(signals: SignalState): Future[Any] = Future.successful()

}
