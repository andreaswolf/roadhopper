package info.andreaswolf.roadhopper

/**
 * This file is a playground to test new ideas for the simulation without the hassle of firing up a complete
 * GraphHopper instance etc.
 */

import akka.actor._
import akka.pattern.ask
import akka.util.Timeout
import info.andreaswolf.roadhopper.simulation._

import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.duration._
import scala.util.{Failure, Success}


class ExtensionComponent(val timer: ActorRef) extends SimulationActor {

	import VelocityControlActor._

	val velocityControl = context.actorOf(Props(new VelocityControlActor(timer)), "velocityControl")

	/**
	 * Handler for [[Start]] messages.
	 * <p/>
	 * The simulation will only continue after the Future has been completed. You can, but don’t need to override this
	 * method in your actor. If you don’t override it, the step will be completed immediately (by the successful Future
	 * returned)
	 */
	override def start()(implicit exec: ExecutionContext): Future[Any] = {
		log.debug("Initializing " + self.path)

		Future.sequence(List(
			velocityControl ? SetTargetVelocity(15),
			timer ? ScheduleStep(100, self)
		))
	}

	override def stepUpdate(time: Int)(implicit exec: ExecutionContext): Future[Any] = {
		log.debug("foo")

		Future.sequence(List(
			timer ? ScheduleStep(time + 100, self),
			velocityControl ? TellVehicleStatus(new VehicleState(1.0, 10.0, Math.PI, None), 100.0),
			{
				if (time == 1000) {
					velocityControl ? SetStopPosition(100.0)
				} else {
					Future.successful()
				}
			}
		))
	}

	override def stepAct(time: Int)(implicit exec: ExecutionContext): Future[Any] = Future {
		log.debug("bar")
	}
}

class AnotherComponent(val timer: ActorRef) extends SimulationActor {

	var foo = 0

	/**
	 * Handler for [[Start]] messages.
	 * <p/>
	 * The simulation will only continue after the Future has been completed. You can, but don’t need to override this
	 * method in your actor. If you don’t override it, the step will be completed immediately (by the successful Future
	 * returned)
	 */
	override def start()(implicit exec: ExecutionContext): Future[Any] = timer ? ScheduleStep(50, self)

	/**
	 * Handler for [[StepUpdate]] messages.
	 * <p/>
	 * The simulation will only continue after the Future has been completed. You can, but don’t need to override this
	 * method in your actor. If you don’t override it, the step will be completed immediately (by the successful Future
	 * returned)
	 *
	 * @param time The current simulation time in milliseconds
	 */
	override def stepUpdate(time: Int)(implicit exec: ExecutionContext): Future[Any] = {
		log.debug(f"AnotherComponent::StepUpdate $time")
		foo += 1
		Future.sequence(List(
			timer ? ScheduleStep(time + 50, self),
			Future {
				val futureStartTime = time
				Thread.sleep(200)
				log.debug(f"== AnotherComponent::stepUpdate(): finished sleeping for $time")
			}
		))
	}

	/**
	 * Handler for [[StepAct]] messages.
	 * <p/>
	 * The simulation will only continue after the Future has been completed. You can, but don’t need to override this
	 * method in your actor. If you don’t override it, the step will be completed immediately (by the successful Future
	 * returned)
	 *
	 * @param time The current simulation time in milliseconds
	 */
	override def stepAct(time: Int)(implicit exec: ExecutionContext): Future[Any] = Future {
		log.debug(f"AnotherComponent::StepAct $time")
		Thread.sleep(150)
		log.debug(f"foo: $foo")
	}
}

class Component(val timer: ActorRef) extends Actor with ActorLogging {

	implicit val timeout = Timeout(10 seconds)
	import context.dispatcher

	val subordinate = context.actorOf(Props(new Subordinate(self)), "subordinate")

	def receive = {
		case Start() =>
			val originalSender = sender()
			timer ? ScheduleStep(50, self) andThen { case x =>
				log.debug("Component::Start() finished")
				originalSender ! true
			}

		case StepUpdate(time) =>
			log.debug(f"Component::StepUpdate $time")
			log.debug("Sleeping 1000ms…")
			Thread.sleep(1000)
			log.debug("1000ms done")
			log.debug("Component: Updating time to " + time)
			sender ! true

		case StepAct(time) =>
			log.debug(f"Component::StepAct $time")
			log.debug("Sleeping 500…")
			Thread.sleep(500)
			log.debug("500ms done")
			log.debug("Component: Got time " + time)
			val originalSender = sender()
			Future sequence List(
				// ask another component a question => we need to directly handle the result inline; this could also be moved
				// to a method, but we cannot use this actor’s receive method, otherwise we cannot use sender() to send the
				// response in subordinate.
				timer ? ScheduleStep(time + 100, self),
				timer ? ScheduleStep(time + 150, self),
				Future {
					val startTime = time
					Thread.sleep(600)
					log.debug(f"Component::StepAct: Finishing future for time $startTime at time $time")
				},
				subordinate ? Question(time) andThen {
					case Success(Answer(x)) =>
						log.debug("Answer for " + time)
					case Success(x) => log.debug(x.toString)
				}
			) andThen {
				case x =>
					log.debug(f"Scheduling request of ${self.path} passed")
					originalSender ! true
			}

		case Answer(time) =>
			log.debug("Answer for " + time)
			sender ! true
	}
}

case class Question(time: Int)

case class Answer(time: Int)

class Subordinate(val component: ActorRef) extends Actor with ActorLogging {
	implicit val timeout = Timeout(10 seconds)

	def receive = {
		case Question(time) =>
			log.debug(f"Subordinate: $time")
			sender ! Answer(time)

	}
}

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
