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
		case Step(time) =>
			println("Driver reached " + time)
			steps += 1
			if (steps < 5) {
				timer ! new TimerRequest(self, time + 40)
			}
	}
}
