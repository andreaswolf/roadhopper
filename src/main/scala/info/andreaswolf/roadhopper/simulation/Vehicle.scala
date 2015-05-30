package info.andreaswolf.roadhopper.simulation

import akka.actor.{Actor, ActorRef}

/**
 * Simple representation of a vehicle
 *
 * @param maxAcceleration The maximum acceleration in meters per square second (m/(s^2^))
 * @param maxSpeed The maximum speed in meters per second
 */
class Vehicle(val maxAcceleration: Float, val maxSpeed: Double) {

	def calculateNewState(currentState: VehicleState, delta: Int): VehicleState = {
		val speed = Math.min(maxSpeed, currentState.speed + (currentState.acceleration * delta / 1000))
		var acceleration = 0.0
		if (speed < maxSpeed) {
			acceleration = currentState.acceleration
		}

		new VehicleState(acceleration, speed, 0.0, driverInput = new DriverInput(0.0))
	}
}

case class Accelerate(value: Double)
case class Decelerate(value: Double)
case class SetAcceleration(value: Double)
case class RequestVehicleStatus()
case class VehicleStatus(acceleration: Double, speed: Double, travelledDistance: Double)

class VehicleActor(val timer: ActorRef) extends Actor {
	var steps = 0

	val maxSpeed = 50.0
	var acceleration = 0.0
	var speed = 0.0

	var lastUpdateTime = 0.0

	var travelledDistance = 0.0

	override def receive: Receive = {
		case Start() =>
			timer ! ScheduleRequest(10)

		case Step(time) =>
			speed += acceleration * (time - lastUpdateTime) / 1000
			if (speed > maxSpeed) {
				acceleration = 0.0
				speed = maxSpeed
			}
			travelledDistance += speed * (time - lastUpdateTime) / 1000

			lastUpdateTime = time

			timer ! ScheduleRequest(time + 10)

		case Accelerate(value) =>
			acceleration += value

		case Decelerate(value) =>
			acceleration -= value

		case SetAcceleration(value) =>
			acceleration = value

		case RequestVehicleStatus() =>
			sender() ! VehicleStatus(acceleration, speed, travelledDistance)
	}
}
