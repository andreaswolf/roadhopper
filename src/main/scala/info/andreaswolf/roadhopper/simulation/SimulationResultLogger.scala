package info.andreaswolf.roadhopper.simulation

import akka.actor.{ActorRef, ActorLogging, Actor}

/**
 * Watches the vehicle status and adds them to the passed result object.
 *
 * @param result
 * @param timer
 * @param interval
 * @param vehicle
 */
class SimulationResultLogger(val result: SimulationResult, val timer: ActorRef, val interval: Int,
                             val vehicle: ActorRef) extends Actor with ActorLogging {

	override def receive: Receive = {
		case Start() =>
			vehicle ! RequestVehicleStatus()

		case Step(time) =>
			vehicle ! RequestVehicleStatus()

		case VehicleStatus(time, state, travelledDistance) =>
			result.setStatus(time, state)

			// Normally, we would be able to schedule a new request directly after receiving a Step signal (as this actor
			// only observes state, but does not need to modify it), but this might lead to weird timing issues if we progress
			// faster than expected (if e.g. the vehicle already progressed beyond the requested time; past status data is not
			// saved in the vehicle).
			// Therefore, we postpone scheduling a new request until here, though that might slow down simulation a bit.
			timer ! ScheduleRequest(time + interval)

	}
}
