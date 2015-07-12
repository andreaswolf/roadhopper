package info.andreaswolf.roadhopper.simulation

import akka.actor.{ActorLogging, Actor, ActorRef}
import info.andreaswolf.roadhopper.road.RoadBendEvaluator


class DriverActor(val timer: ActorRef, val vehicle: ActorRef, val journey: ActorRef) extends Actor with ActorLogging {
	var steps = 0
	protected var currentTime = 0

	val bendEvaluator = new RoadBendEvaluator

	override def receive: Receive = {
		case Start() =>
			log.debug("Driver starting")
			vehicle ! Accelerate(1.0)
			timer ! ScheduleRequest(40)

		case Step(time) =>
			currentTime = time
			vehicle ! RequestVehicleStatus()

		case RoadAhead(time, roadParts) =>
			if (currentTime % 2000 == 0) {
				log.debug(roadParts.length + " road segment(s) immediately ahead; " + currentTime)
				log.debug(bendEvaluator.findBend(roadParts).toString())
			}

			timer ! ScheduleRequest(currentTime + 40)

		case VehicleStatus(time, state, travelledDistance) =>
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
			journey ! RequestRoadAhead(travelledDistance.toInt)
	}
}
