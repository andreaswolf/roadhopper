package info.andreaswolf.roadhopper.simulation

import akka.actor.{ActorLogging, ActorRef}
import akka.pattern.ask

import scala.collection.mutable.ListBuffer
import scala.concurrent.{Future, ExecutionContext}
import scala.util.Success


@deprecated
class VehicleStatusMonitor(val timer: ActorRef, val interval: Int, val vehicle: ActorRef) extends SimulationActor with ActorLogging {

	/**
	 * Handler for [[Start]] messages.
	 * <p/>
	 * The simulation will only continue after the Future has been completed. You can, but don’t need to override this
	 * method in your actor. If you don’t override it, the step will be completed immediately (by the successful Future
	 * returned)
	 */
	override def start()(implicit exec: ExecutionContext): Future[Any] = timer ? ScheduleStep(10, self)

	/**
	 * Handler for [[StepAct]] messages.
	 * <p/>
	 * The simulation will only continue after the Future has been completed. You can, but don’t need to override this
	 * method in your actor. If you don’t override it, the step will be completed immediately (by the successful Future
	 * returned)
	 */
	override def stepAct()(implicit exec: ExecutionContext): Future[Any] = {
		val futures = new ListBuffer[Future[Any]]()

		futures.append(vehicle ? GetStatus() andThen {
			case Success(status @ JourneyStatus(statusTime, state, travelledDistance)) =>
				log.debug(f"Vehicle at $time: speed = ${status.vehicleState.speed}%3.2f, acceleration = ${status.vehicleState.acceleration}%1.1f,"
								+ f" distance = ${status.travelledDistance}%3.2f, orientation = ${status.vehicleState.orientation.toDegrees}%3.1f")
		})
		// only schedule if the journey has not ended
		futures.append(timer ? ScheduleStep(time + interval, self))

		Future.sequence(futures.toList)
	}
}
