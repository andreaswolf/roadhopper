package info.andreaswolf.roadhopper.simulation

import akka.actor.{ActorLogging, ActorRef}
import akka.pattern.ask
import info.andreaswolf.roadhopper.road.RoadBendEvaluator

import scala.concurrent.{Future, ExecutionContext}
import scala.util.Success


class TwoStepDriverActor(val timer: ActorRef, val vehicle: ActorRef, val journey: ActorRef)
	extends SimulationActor with ActorLogging {

	var steps = 0
	protected var currentTime = 0

	val bendEvaluator = new RoadBendEvaluator


	/**
	 * Handler for [[Start]] messages.
	 * <p/>
	 * The simulation will only continue after the Future has been completed. You can, but don’t need to override this
	 * method in your actor. If you don’t override it, the step will be completed immediately (by the successful Future
	 * returned)
	 */
	override def start()(implicit exec: ExecutionContext): Future[Any] = Future {
		vehicle ? Accelerate(1.0)
		timer ? ScheduleStep (40, self)
	}

	/**
	 * Handler for [[StepUpdate]] messages.
	 * <p/>
	 * The simulation will only continue after the Future has been completed. You can, but don’t need to override this
	 * method in your actor. If you don’t override it, the step will be completed immediately (by the successful Future
	 * returned)
	 *
	 * @param time The current simulation time in milliseconds
	 */
	override def stepUpdate(time: Int)(implicit exec: ExecutionContext): Future[Any] = Future {
		currentTime = time
		// Get the current vehicle status and act accordingly
		vehicle ? GetStatus() andThen {
			case Success(VehicleStatus(_, state, travelledDistance)) => {
				if (travelledDistance > 10000) {
					if (state.speed < -0.25) {
						vehicle ! SetAcceleration(1.0)
					} else if (state.speed > 0.25) {
						vehicle ! SetAcceleration(-1.0)
					} else {
						vehicle ! SetAcceleration(0.0)
						timer ! StopSimulation()
					}
				}

				// if we have a vehicle status, we also have the current position and can thus get the road ahead
				journey ? RequestRoadAhead andThen {
					case Success(RoadAhead(_, roadParts)) =>
						if (currentTime % 2000 == 0) {
							log.debug(roadParts.length + " road segment(s) immediately ahead; " + currentTime)
							log.debug(bendEvaluator.findBend(roadParts).toString())
						}
				}
			}
		}

		timer ? ScheduleStep(currentTime + 40, self)
	}

}
