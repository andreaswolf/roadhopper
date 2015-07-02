package info.andreaswolf.roadhopper.simulation

import akka.actor.{ActorLogging, ActorRef, Actor}
import org.slf4j.LoggerFactory

class VehicleStatusMonitor(val timer: ActorRef, val interval: Int, val vehicle: ActorRef) extends Actor with ActorLogging {

	override def receive: Receive = {
		case Start() =>
			timer ! ScheduleRequest(interval)

		case Step(time) =>
			vehicle ! RequestVehicleStatus()

		case VehicleStatus(time, state, travelledDistance) =>
			log.debug(f"Vehicle at $time: speed = ${state.speed}%3.2f, acceleration = ${state.acceleration}%1.1f,"
				+ f" distance = $travelledDistance%3.2f, orientation = ${state.orientation.toDegrees}%3.1f")

			// Normally, we would be able to schedule a new request directly after receiving a Step signal (as this actor
			// only observes state, but does not need to modify it), but this might lead to weird timing issues if we progress
			// faster than expected (if e.g. the vehicle already progressed beyond the requested time; past status data is not
			// saved in the vehicle).
			// Therefore, we postpone scheduling a new request until here, though that might slow down simulation a bit.
			timer ! ScheduleRequest(time + interval)

	}

}
