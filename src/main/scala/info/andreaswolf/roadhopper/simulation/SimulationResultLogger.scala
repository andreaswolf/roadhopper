package info.andreaswolf.roadhopper.simulation

import akka.actor.{ActorRef, ActorLogging}
import akka.pattern.ask

import scala.concurrent.{Future, ExecutionContext}
import scala.util.Success

/**
 * Watches the vehicle status and adds them to the passed result object.
 *
 * @param result
 * @param timer
 * @param interval
 * @param vehicle
 */
class SimulationResultLogger(val result: SimulationResult, val timer: ActorRef, val interval: Int,
                             val vehicle: ActorRef) extends SimulationActor with ActorLogging {


	/**
	 * Handler for [[Start]] messages.
	 * <p/>
	 * The simulation will only continue after the Future has been completed. You can, but don’t need to override this
	 * method in your actor. If you don’t override it, the step will be completed immediately (by the successful Future
	 * returned)
	 */
	override def start()(implicit exec: ExecutionContext): Future[Any] = {
		Future.sequence(
			List(
				vehicle ? GetStatus() andThen {
					case Success(status @ JourneyStatus(statusTime, state, travelledDistance)) =>
						result.setStatus(0, status.vehicleState);
				},
				timer ? ScheduleStep(interval, self)
			)
		)
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
	override def stepAct(time: Int)(implicit exec: ExecutionContext): Future[Any] = {
		Future.sequence(
			List(
				vehicle ? GetStatus() andThen {
					case Success(status @ JourneyStatus(statusTime, state, travelledDistance)) =>
						result.setStatus(status.time, status.vehicleState)
				},
				timer ? ScheduleStep(time + interval, self)
			)
		)
	}
}
