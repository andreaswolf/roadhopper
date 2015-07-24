/*
 * Copyright (c) 2015 Andreas Wolf
 *
 * Permission is hereby granted, free of charge, to any person obtaining
 * a copy of this software and associated documentation files (the "Software"),
 * to deal in the Software without restriction, including without limitation
 * the rights to use, copy, modify, merge, publish, distribute, sublicense,
 * and/or sell copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included
 * in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS
 * OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package info.andreaswolf.roadhopper.simulation

import akka.actor._

object VelocityControlActor {

	/**
	 * The driving mode is the state of our velocity control.
	 */
	sealed trait DrivingMode

	case object Idle extends DrivingMode

	case object Free extends DrivingMode

	case object StopAtPosition extends DrivingMode


	/**
	 * The data is additional information that is carried around between the events. It can be passed to a state using
	 *
	 * <pre>
	 *   goto(State) using DataClass(arguments)
	 * </pre>
	 *
	 * The same works for [[FSM.stay()]]. The data classes should not be used from the outside.
	 */
	sealed trait Data

	case object Uninitialized extends Data

	case class TargetVelocity(velocity: Int) extends Data

	/**
	 * TODO check if we should optionally keep the velocity here for speeding up again after the stop.
	 */
	case class StopPosition(position: Double) extends Data


	/**
	 * Events are messages sent to the FSM from the outside. The FSM can receive any message (wrapped in an Event() case
	 * class), but to better expose the protocol, we explicitly define the allowed message types here.
	 *
	 * The class names should involve an action (like "Set…" or "Tell…")
	 */
	sealed trait Event

	/**
	 * Inform the velocity control about the current vehicle state. Time is not included, as everything is
	 * synchronous and we also don’t record the status here.
	 */
	case class TellVehicleStatus(state: VehicleState, travelledDistance: Double) extends Event

	/**
	 * Set the velocity we should aim for. There is no guarantee that this will ever be reached, but if possible,
	 * it should.
	 */
	case class SetTargetVelocity(velocity: Int) extends Event

	/**
	 * Sets the position where we should stop. The vehicle will stop in any case, unless a different message is received
	 * before; if it is impossible to stop at the location (because it is too near already), the vehicle will stop at
	 * the next feasible location
	 */
	case class SetStopPosition(position: Double) extends Event
}


/**
 * The velocity control state machine.
 *
 * See http://chariotsolutions.com/blog/post/fsm-actors-akka/ for a good overview of how FSMs in Akka work
 */
class VelocityControlActor(val timer: ActorRef) extends LoggingFSM[VelocityControlActor.DrivingMode, VelocityControlActor.Data] {

	import VelocityControlActor._

	startWith(Idle, Uninitialized)

	// TODO remove these properties
	var _targetVelocity = 0.0
	var _vehicleStatus = null


	when(Idle) {
		case Event(TellVehicleStatus(state, travelledDistance), _) =>
			log.debug("Received vehicle status")
			// No need to do anything here, as we are idle.
			stay() replying(true)

		case Event(SetTargetVelocity(velocity), _) =>
			log.debug("Setting target velocity")
			targetVelocity = velocity

			// replying() is necessary because the FSM user uses ask() to keep control flow in sync
			goto(Free) using TargetVelocity(velocity) replying (true)

		case x =>
			log.warning(s"Unhandled event in status Idle: $x")
			stay() replying (true)
	}

	when(Free) {
		case Event(TellVehicleStatus(state, travelledDistance), TargetVelocity(velocity)) =>
			log.debug(s"Received vehicle status; aiming for $velocity")

			stay() replying(true)

		case Event(TellVehicleStatus(state, travelledDistance), StopPosition(position)) =>
			log.debug(s"Received vehicle status; stopping at $position")

			stay() replying(true)

		case Event(SetTargetVelocity(velocity), _) =>
			log.debug("Received SetTargetVelocity in Free")

			// replying() is necessary because the FSM user uses ask() to keep control flow in sync
			stay() replying (true)

		case Event(SetStopPosition(position), _) =>
			log.debug("Received StopAtPosition in Free")

			// Change state
			goto(StopAtPosition) using (StopPosition(position)) replying (true)

		case x =>
			log.warning(s"Unhandled event in status Free: $x")
			stay() replying (true)
	}

	when(StopAtPosition) {
		case Event(TellVehicleStatus(state, travelledDistance), StopPosition(position)) =>
			log.debug(s"Received vehicle status; stopping at $position")

			// TODO implement a model of the process to stop the vehicle

			stay() replying(true)
	}

	onTransition {
		case Idle -> Free =>
			log.debug("Switching state: Idle → Free")

			nextStateData match {
				case TargetVelocity(velocity) =>
					targetVelocity = velocity
				case x => log.warning("Unrecognized input in transition Idle→Free: " + x)
			}

		case Free -> StopAtPosition =>
			log.debug("Switching state: Free → StopAtPosition")
	}


	whenUnhandled {
		case Event(e, s) =>
			log.warning("received unhandled request {} in state {}/{}", e, stateName, s)
			stay()
	}

//
//	/**
//	 * Handler for [[Start]] messages.
//	 * <p/>
//	 * The simulation will only continue after the Future has been completed. You can, but don’t need to override this
//	 * method in your actor. If you don’t override it, the step will be completed immediately (by the successful Future
//	 * returned)
//	 */
//	override def start()(implicit exec: ExecutionContext): Future[Any] = {
//		timer ? ScheduleStep(100, self)
//	}
//
//	/**
//	 * Handler for [[StepUpdate]] messages.
//	 * <p/>
//	 * The simulation will only continue after the Future has been completed. You can, but don’t need to override this
//	 * method in your actor. If you don’t override it, the step will be completed immediately (by the successful Future
//	 * returned)
//	 *
//	 * @param time The current simulation time in milliseconds
//	 */
//	override def stepUpdate(time: Int)(implicit exec: ExecutionContext): Future[Any] = {
//		timer ? ScheduleStep(time + 100, self)
//	}


	def targetVelocity = _targetVelocity
	def targetVelocity_=(velocity: Double): Unit = {
		_targetVelocity = velocity
		log.debug(s"Setting target velocity to ${_targetVelocity}")
	}


	log.debug("Creating velocity control actor")
	initialize()

}
