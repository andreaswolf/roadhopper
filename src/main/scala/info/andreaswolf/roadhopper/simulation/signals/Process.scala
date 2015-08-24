/*
 * Copyright (c) 2015 Andreas Wolf
 *
 * See te LICENSE file in the project root for further copyright information.
 */

package info.andreaswolf.roadhopper.simulation.signals

import akka.actor.{ActorRef, Actor}
import akka.util.Timeout
import info.andreaswolf.roadhopper.simulation.{ExtensibleReceiver, TellTime}
import info.andreaswolf.roadhopper.simulation.signals.Process._

import scala.concurrent.Future
import scala.concurrent.duration._

object Process {

	case class Invoke(signalState: SignalState)

}

/**
 * A process is a component that uses one or more signals for calculations.
 *
 * The process subscribes to the signals it needs for its calculations and is notified by the signal bus each time a
 * signal changes.
 *
 * @param bus The signal bus instance. Optional.
 */
abstract class Process(val bus: ActorRef) extends Actor with ExtensibleReceiver {

	implicit val timeout = Timeout(10 seconds)
	import context.dispatcher

	var time: Int = 0

	/**
	 * The list of message handlers.
	 * <p/>
	 * See [[registerReceiver()]] for more information on how to add your own handlers.
	 * <p/>
	 * WARNING: it is undefined in which order the case statements from the different Receive instances will be invoked
	 * (as the list is not ordered). If we need to explicitly override any of the cases defined here, we need to convert
	 * this List() into something with explicit ordering.
	 */
	registerReceiver({
		case TellTime(_time) =>
			val oldTime = time
			val originalSender = sender()
			time = _time
			timeAdvanced(oldTime, _time) andThen {
				case x =>
					originalSender ! true
			}

		case Invoke(signalState) =>
			val originalSender = sender()
			invoke(signalState) andThen {
				case x =>
					originalSender ! true
			}

	})


	def timeAdvanced(oldTime: Int, newTime: Int): Future[Any] = Future.successful()

	/**
	 * The central routine of a process. This is invoked whenever a subscribed signal’s value changes.
	 */
	def invoke(signals: SignalState): Future[Any] = Future.successful()

	def invoke(signals: SignalState, bus: ActorRef): Future[Any] = Future.successful()

}
