package info.andreaswolf.roadhopper.simulation

import akka.actor.{Actor, ActorRef}

case class Accelerate(value: Double)
case class Decelerate(value: Double)
case class SetAcceleration(value: Double)
case class RequestVehicleStatus()
// TODO pass a VehicleState object instead
case class VehicleStatus(time: Double, state: VehicleState, travelledDistance: Double)

/**
 * Lets the vehicle turn by the given angle (in radians)
 *
 * @param delta
 */
case class Turn(delta: Double)


class VehicleActor(val timer: ActorRef, val initialOrientation: Double = 0.0) extends Actor {
	var steps = 0

	val maxSpeed = 50.0
	var acceleration = 0.0
	var speed = 0.0

	var orientation = initialOrientation

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

		case Turn(delta) =>
			orientation += delta
			println("Vehicle turned by " + delta.toDegrees + " to " + orientation.toDegrees)

		case RequestVehicleStatus() =>
			sender() ! VehicleStatus(lastUpdateTime, new VehicleState(acceleration, speed, orientation), travelledDistance)
	}
}
