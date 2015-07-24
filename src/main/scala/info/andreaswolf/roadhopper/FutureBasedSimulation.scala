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
