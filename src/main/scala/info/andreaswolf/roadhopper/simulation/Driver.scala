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


class DriverActor(val timer: ActorRef) extends Actor {
	var steps = 0

	override def receive: Receive = {
		case Start() =>
			println("Driver starting")
			timer ! ScheduleRequest(10)

		case Step(time) =>
			println("Driver reached " + time)
			steps += 1
			if (steps < 5) {
				timer ! ScheduleRequest(time + 40)
			} else {
				timer ! Pass()
			}
	}
}
