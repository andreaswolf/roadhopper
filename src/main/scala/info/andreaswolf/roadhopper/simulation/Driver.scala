package info.andreaswolf.roadhopper.simulation

import akka.actor.{Actor, ActorRef}

/**
 * A vehicle driver, responsible for steering the vehicle.
 */
class Driver {

	def changeVehicleInput(currentStep: SimulationStep): DriverInput = {
		// TODO adjust steering wheel
		new DriverInput(0.0)
	}

}


class DriverActor(val timer: ActorRef, val vehicle: ActorRef, val journey: ActorRef) extends Actor {
	var steps = 0
	protected var currentTime = 0

	override def receive: Receive = {
		case Start() =>
			println("Driver starting")
			vehicle ! Accelerate(1.0)
			timer ! ScheduleRequest(40)

		case Step(time) =>
			currentTime = time
			vehicle ! RequestVehicleStatus()

		case RoadAhead(time, roadParts) =>
			if (currentTime % 2000 == 0) {
				println(roadParts.length + " road segment immediately ahead; " + currentTime)
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

/**
 * A bend in the road, i.e. a change of direction over one or multiple segments
 *
 * @param length The length of the arc
 * @param angle The turn angle
 */
class RoadBend(val length: Double, val direction: TurnDirection, val angle: Double) {

}

class TurnDirection extends Enumeration {
	val LEFT, RIGHT = Value
}
