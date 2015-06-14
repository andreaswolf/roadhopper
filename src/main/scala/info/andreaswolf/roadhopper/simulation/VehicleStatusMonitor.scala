package info.andreaswolf.roadhopper.simulation

import akka.actor.{ActorRef, Actor}
import org.slf4j.LoggerFactory

class VehicleStatusMonitor(val timer: ActorRef, val interval: Int, val vehicle: ActorRef) extends Actor {

	val logger = LoggerFactory.getLogger(this.getClass)

	override def receive: Receive = {
		case Start() =>
			timer ! ScheduleRequest(interval)

		case Step(time) =>
			timer ! ScheduleRequest(time + interval)
			vehicle ! RequestVehicleStatus()

		case VehicleStatus(time, acceleration, speed, distance, orientation) =>
			logger.info(f"Vehicle at $time: speed = $speed%3.2f, acceleration = $acceleration%1.1f, distance = $distance%3.2f, orientation = ${orientation.toDegrees}%3.1f")

	}

}
