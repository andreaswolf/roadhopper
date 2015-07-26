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
import akka.pattern.ask
import akka.util.Timeout
import info.andreaswolf.roadhopper.road.{RoadSegment, StopSign}

import scala.concurrent.{Await, Future}
import scala.util.Success
import scala.concurrent.duration._


/**
 * Companion for the velocity control state machine. Holds the driving modes
 */
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
	 * goto(State) using DataClass(arguments)
	 * </pre>
	 *
	 * The same works for [[FSM.stay()]]. The data classes should not be used from the outside.
	 */
	sealed abstract class Data(val status: JourneyStatus, val position: Double)

	case object Uninitialized extends Data(new JourneyStatus(0, VehicleState.getDefault(), 0.0), 0)

	case class TargetVelocity(velocity: Int, override val status: JourneyStatus)
		extends Data(status, status.travelledDistance) {

		def this(velocity: Int, data: Data) = this(velocity, data.status)
	}

	/**
	 * @param velocity The target velocity to aim for before/after stopping
	 */
	case class StopPosition(stopPosition: Double, velocity: Double = 0.0, override val status: JourneyStatus)
		extends Data(status, status.travelledDistance) {

		// TODO do we need this?
		def this(stopPosition: Double, data: Data) = this(stopPosition, 0.0, data.status)

		def this(stopPosition: Double, data: TargetVelocity) = this(stopPosition, data.velocity, data.status)

		/**
		 * Copy constructor for updating the current JourneyStatus
		 *
		 * @param data The current state data
		 */
		def this(journeyStatus: JourneyStatus, data: StopPosition) = this(data.stopPosition, data.velocity, journeyStatus)
	}


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
	 * TODO: use JourneyStatus instead, remove time information from it
	 */
	case class TellVehicleStatus(state: VehicleState, travelledDistance: Double) extends Event {
		def this(state: VehicleState, journeyState: JourneyState) = this(state, journeyState.travelledDistance)
	}

	/**
	 * Inform the velocity control about the road ahead of the vehicle
	 */
	case class TellRoadAhead(road: RoadAhead) extends Event

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
class VelocityControlActor(val timer: ActorRef, val vehicle: ActorRef)
	extends LoggingFSM[VelocityControlActor.DrivingMode, VelocityControlActor.Data] {

	implicit val timeout = Timeout(10 seconds)

	import context.dispatcher
	import VelocityControlActor._

	startWith(Idle, Uninitialized)


	/**
	 * Event handlers for the Idle state
	 */
	when(Idle) {
		case Event(TellVehicleStatus(state, travelledDistance), _) =>
			log.debug("Received vehicle status")
			// No need to do anything here, as we are idle.
			stay() replying (true)

		case Event(SetTargetVelocity(velocity), data) =>
			log.debug("Setting target velocity")

			// replying() is necessary because the FSM user uses ask() to keep control flow in sync
			goto(Free) using new TargetVelocity(velocity, data) replying (true)


		case Event(TellRoadAhead(road), _) =>
			// Ignoring information about the road in idle mode, as we will get it in driving mode anyways
			stay() replying (true)

		case x =>
			log.warning(s"Unhandled event in status Idle: $x")
			stay() replying (true)
	}

	/**
	 * Event handlers for the Free state
	 */
	when(Free) {
		case Event(TellVehicleStatus(state, travelledDistance), TargetVelocity(velocity, _)) =>
			log.debug(s"Received vehicle status; aiming for $velocity")

			adjustAccelerationForFreeDriving(travelledDistance, velocity, state)

			// create a new TargetVelocity object as it must contain the currently travelled distance
			stay() using (new TargetVelocity(velocity, new JourneyStatus(0, state, travelledDistance))) replying (true)

		case Event(TellVehicleStatus(state, travelledDistance), StopPosition(position, _, _)) =>
			log.debug(s"Received vehicle status; stopping at $position")
			// TODO this should not be reachable

			stay() replying (true)

		case Event(TellRoadAhead(road), data @ TargetVelocity(velocity, _)) =>
			// check if there is a stop sign ahead
			var length = 0.0
			def checkStopSign(roadPart: RoadSegment): Boolean = {
				roadPart.roadSign.isDefined && roadPart.roadSign.get.isInstanceOf[StopSign]
			}
			var newState: State = null
			for (roadPart <- road.roadParts) {
				length += roadPart.length
				// ignore ultra-short road segments before a stop sign (these will likely mean that we already stopped and
				// are just starting again)
				if (newState == null && checkStopSign(roadPart) && length > 2.0) {
					newState = (goto(StopAtPosition)
						using (new StopPosition(data.status.travelledDistance + length, data))
						replying (true)
					)
				}
			}
			// if the end of the journey comes close, we need to set a stop point there
			if (newState == null && road.isEnding) {
				newState = (goto(StopAtPosition)
					using (new StopPosition(data.status.travelledDistance + road.length, data))
					replying (true)
				)
			}
			if (newState == null) {
				newState = stay() replying (true)
			}
			newState

		case Event(SetTargetVelocity(velocity), _) =>
			log.debug("Received SetTargetVelocity in Free")

			// replying() is necessary because the FSM user uses ask() to keep control flow in sync
			stay() replying (true)

		case Event(SetStopPosition(position), data) =>
			log.debug("Received StopAtPosition in Free")

			// Change state
			goto(StopAtPosition) using (new StopPosition(position, data)) replying (true)

		case x =>
			log.warning(s"Unhandled event in status Free: $x")
			stay() replying (true)
	}

	when(StopAtPosition) {
		case Event(TellVehicleStatus(state, travelledDistance), stateData @ StopPosition(stopPosition, _, _)) =>
			log.debug(s"Received vehicle status; stopping at $stopPosition")

			adjustAccelerationForStopAtPosition(travelledDistance, stateData, state)


		case Event(TellRoadAhead(road), _) =>
			// TODO no need to do anything here?

			stay() replying (true)
	}

	onTransition {
		case Idle -> Free =>
			log.debug("Switching state: Idle → Free")

			(vehicle ? SetAcceleration(1.0)).andThen {
				case Success(x) =>
					log.info("Acceleration was set")
			}
			log.debug("After setting acceleration")

		case Free -> StopAtPosition =>
			log.debug("Switching state: Free → StopAtPosition")
	}


	whenUnhandled {
		case Event(e, s) =>
			log.warning("received unhandled request {} in state {}/{}", e, stateName, s)
			stay()
	}


	def adjustAccelerationForFreeDriving(currentPosition: Double, targetVelocity: Double, vehicleState: VehicleState) = {

		// TODO check remaining distance on journey
		val delta_v = (targetVelocity - vehicleState.speed)

		val accelerationFuture: Future[Any] = {
			delta_v match {
				case x if x > 15 => vehicle ? SetAcceleration(2.0)
				case x if x > 5 => vehicle ? SetAcceleration(1.0)
				case x if x > 1 => vehicle ? SetAcceleration(0.5)
				case x if x > 0.25 => vehicle ? SetAcceleration(0.25)
				case x if x > 0 => vehicle ? SetAcceleration(0.05)
				case x if x < 0 => vehicle ? SetAcceleration(-0.05)
			}
		}

	}

	def adjustAccelerationForStopAtPosition(currentPosition: Double, stateData: StopPosition, vehicleState: VehicleState): State = {
		val distanceToStop = stateData.stopPosition - currentPosition

		// "circuit breaker" for testing; can be removed later on
		if (distanceToStop < 0) {
			log.error("Threw circuit breaker!")
			context.system.shutdown()
		}

		// stop one meter before the actual point
		val requiredDeceleration = vehicleState.speed * vehicleState.speed / (2 * (distanceToStop - 1))

		val decelerationFuture: Future[Any] = {
			requiredDeceleration match {
				case x if x > 1.5 => vehicle ? SetAcceleration(-2.0)
				case x if x > 1.0 => vehicle ? SetAcceleration(0.0)
				// TODO 10m is just a guess; check this again
				case x if x < 0.4 && distanceToStop < 10.0 => vehicle ? SetAcceleration(-0.2)
				case x if x < 0.05 && distanceToStop < 10.0 => vehicle ? SetAcceleration(-0.05)
				case x if x < 0 => vehicle ? SetAcceleration(0.05)
				case x => Future.successful() // do nothing
			}
		}
		log.debug(f"Remaining until stop position: $distanceToStop%.2f, current speed: ${vehicleState.speed}%.2f, " +
			f"required deceleration: $requiredDeceleration%.2f")

		Await.result(decelerationFuture, 1 second)

		val journeyStatus: JourneyStatus = new JourneyStatus(0, vehicleState, currentPosition)

		if (vehicleState.speed == 0.0 && distanceToStop < 1.1) {
			// TODO use the real target velocity here; also use the correct time in JourneyStatus or get rid of the time completely
			goto(Free) using (TargetVelocity(15, journeyStatus)) replying (true)
		} else {
			stay() using (new StopPosition(journeyStatus, stateData)) replying (true)
		}
	}



	log.debug("Creating velocity control actor")
	initialize()

}
