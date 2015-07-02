package info.andreaswolf.roadhopper.simulation

import akka.actor.{ActorLogging, ActorRef, Actor}
import org.slf4j.LoggerFactory

class VehicleStatusMonitor(val timer: ActorRef, val interval: Int, val vehicle: ActorRef) extends Actor with ActorLogging {

	override def receive: Receive = {
		case Start() =>
			timer ! ScheduleRequest(interval)

		case Step(time) =>
			timer ! ScheduleRequest(time + interval)
			vehicle ! RequestVehicleStatus()

		case VehicleStatus(time, state, travelledDistance) =>
			log.debug(f"Vehicle at $time: speed = ${state.speed}%3.2f, acceleration = ${state.acceleration}%1.1f,"
				+ f" distance = $travelledDistance%3.2f, orientation = ${state.orientation.toDegrees}%3.1f")

	}

}
