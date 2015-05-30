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


class DriverActor(val timer: ActorRef, val vehicle: ActorRef) extends Actor {
	var steps = 0

	override def receive: Receive = {
		case Start() =>
			println("Driver starting")
			timer ! ScheduleRequest(10)
			vehicle ! Accelerate(1.0)

		case Step(time) =>
			//println("Driver reached " + time)

			vehicle ! RequestVehicleStatus()
			timer ! ScheduleRequest(time + 40)

		case VehicleStatus(currentAcceleration, currentSpeed, travelledDistance) =>
			//println("Checking vehicle status")
			if (travelledDistance > 10000) {
				if (currentSpeed < -0.25) {
					vehicle ! SetAcceleration(1.0)
				} else if (currentSpeed > 0.25) {
					vehicle ! SetAcceleration(-1.0)
				} else {
					vehicle ! SetAcceleration(0.0)
					timer ! StopSimulation()
				}
			}
	}
}
