package info.andreaswolf.roadhopper.simulation

import akka.actor._
import akka.pattern.{AskTimeoutException, ask}
import info.andreaswolf.roadhopper.road.RoadBendEvaluator
import VelocityControlActor.{TellVehicleStatus, SetTargetVelocity, TellRoadAhead}

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}


class TwoStepDriverActor(val timer: ActorRef, val vehicle: ActorRef, val journey: ActorRef)
	extends SimulationActor with ActorLogging {

	var steps = 0
	protected var currentTime = 0

	val bendEvaluator = new RoadBendEvaluator

	val velocityControl = context.actorOf(Props(new VelocityControlActor(timer, vehicle)), "velocityControl")

	/**
	 * Handler for [[Start]] messages.
	 * <p/>
	 * The simulation will only continue after the Future has been completed. You can, but don’t need to override this
	 * method in your actor. If you don’t override it, the step will be completed immediately (by the successful Future
	 * returned)
	 */
	override def start()(implicit exec: ExecutionContext): Future[Any] = {
		Future.sequence(List(
			velocityControl ? SetTargetVelocity(20),
			timer ? ScheduleStep(40, self)
		))
	}

	/**
	 * Handler for [[StepUpdate]] messages.
	 * <p/>
	 * The simulation will only continue after the Future has been completed. You can, but don’t need to override this
	 * method in your actor. If you don’t override it, the step will be completed immediately (by the successful Future
	 * returned)
	 */
	override def stepAct()(implicit exec: ExecutionContext): Future[Any] = {
		currentTime = time

		// Get the current vehicle status and act accordingly
		val statusFuture: Future[JourneyStatus] = (vehicle ? GetStatus()).asInstanceOf[Future[JourneyStatus]]

		// forward data from vehicle to velocity control
		val forwardStatus = statusFuture flatMap { status: JourneyStatus =>
			velocityControl ? new TellVehicleStatus(status.vehicleState, status.travelledDistance)
		}
		val forwardRoad = statusFuture flatMap { status: JourneyStatus =>
			(journey ? RequestRoadAhead(status.travelledDistance.toInt)).asInstanceOf[Future[RoadAhead]]
		} flatMap { road: RoadAhead =>
			velocityControl ? TellRoadAhead(road)
		}

		// TODO this block is not required anymore, except for road bends (but these could also be evaluated in velocityControl)
		// Factory method for the road check future. Necessary because we only get the required parameter as the result
		// of an earlier future, but create a new future here by calling another actor (as opposed to the other requests
		// above, which just check the future’s results)
		// TODO check how we can react to failures from the previous future here
		def checkRoadAhead(status: JourneyStatus): Future[Any] = {
			journey ? RequestRoadAhead(status.travelledDistance.toInt) andThen {
				case Success(RoadAhead(_, roadParts, _)) => {
					// Attention: if this code ever needs to trigger further actions, it must be mapped in a similar structure
					// as the surrounding method, as we need the futures from these actions on the top level
					val startTime = currentTime
					log.debug(roadParts.length + " road segment(s) immediately ahead; " + currentTime)
					if (currentTime % 2000 == 0) {
						log.debug(bendEvaluator.findBend(roadParts).toString())
						log.debug(f"Road evaluation finished; $startTime -> $currentTime")
					}
				}
			}
		}
		// using flatMap inserts the first future’s result as the second ones parameter
		// we cannot directly use the future’s andThen() here, as we create the second future inside and need to
		val journeyFuture = statusFuture flatMap checkRoadAhead

		Future.sequence(List(
			statusFuture,
			forwardStatus,
			forwardRoad,
			timer ? ScheduleStep(currentTime + 40, self)
		))
	}

}

