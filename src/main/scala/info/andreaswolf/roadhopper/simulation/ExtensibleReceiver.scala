/*
 * Copyright (c) 2015 Andreas Wolf
 *
 * See te LICENSE file in the project root for further copyright information.
 */

package info.andreaswolf.roadhopper.simulation

import akka.actor.Actor


/**
 * Base structure for an Actor receive that can be defined by multiple classes in a hierarchy.
 */
trait ExtensibleReceiver {

	var _receive: List[Actor.Receive] = List()

	/**
	 * Registers a new receiver. Call with a partial function to make the actor accept additional types of messages.
	 * <p/>
	 * Example:
	 * <p/>
	 * <pre>
	 * registerReceiver {
	 *   case MyMessage() =>
	 *     // code to handle MyMessage()
	 * }</pre>
	 * <p/>
	 * WARNING the execution order of the receive functions is currently undefined. If you need to override an existing
	 *         message handler, make sure to fix this issue first!
	 */
	def registerReceiver(receive: Actor.Receive) { _receive = receive :: _receive }

	/**
	 * The receive function, must not be overridden. Instead, register your own receiver function with
	 * [[registerReceiver()]]
	 */
	final def receive =  _receive reduce {_ orElse _}
}
