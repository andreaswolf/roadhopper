package info.andreaswolf.roadhopper.simulation

import akka.actor.{ActorLogging, Actor, ActorRef}
import akka.pattern.ask
import com.graphhopper.util.shapes.GHPoint3D

import scala.concurrent.{Future, ExecutionContext}

case class Accelerate(value: Double)

case class Decelerate(value: Double)

case class SetAcceleration(value: Double)

case class RequestJourneyStatus()

case class GetStatus()

case class JourneyStatus(time: Int, vehicleState: VehicleState, travelledDistance: Double) {
	def this(time: Int, vehicleState: VehicleState, journeyState: JourneyState) =
		this(time, vehicleState, journeyState.travelledDistance)
}

case class UpdatePosition(position: GHPoint3D)

/**
 * Lets the vehicle turn by the given angle (in radians)
 *
 * @param delta
 */
case class Turn(delta: Double)


class TwoStepVehicleActor(val timer: ActorRef, val initialOrientation: Double = 0.0,
                          initialPosition: Option[GHPoint3D] = None) extends SimulationActor with ActorLogging {

	val maxSpeed = 50.0
	var acceleration = 0.0
	var speed = 0.0

	var orientation = initialOrientation
	var position: Option[GHPoint3D] = initialPosition

	var lastUpdateTime = 0

	var travelledDistance = 0.0

	registerReceiver({
		case Accelerate(value) =>
			acceleration += value

		case Decelerate(value) =>
			acceleration -= value

		case SetAcceleration(value) =>
			log.debug(s"Setting acceleration to $value m/s")
			acceleration = value
			sender() ! true

		case Turn(delta) =>
			orientation += delta
			log.debug(f"Vehicle turned by ${delta.toDegrees}%.2f° to ${orientation.toDegrees}%.2f°")

		/**
		 * Knowledge about the vehicle’s position is managed by the Journey
		 */
		case UpdatePosition(pos) =>
			position = Some(pos)

		case GetStatus() =>
			sender() ! new JourneyStatus(lastUpdateTime,
				new VehicleState(acceleration, speed, orientation, position), new JourneyState(travelledDistance)
			)
	})

	/**
	 * Handler for [[Start]] messages.
	 * <p/>
	 * The simulation will only continue after the Future has been completed. You can, but don’t need to override this
	 * method in your actor. If you don’t override it, the step will be completed immediately (by the successful Future
	 * returned)
	 */
	override def start()(implicit exec: ExecutionContext): Future[Any] = Future {
		timer ? ScheduleStep(10, self)
	}

	/**
	 * Handler for [[StepUpdate]] messages.
	 * <p/>
	 * The simulation will only continue after the Future has been completed. You can, but don’t need to override this
	 * method in your actor. If you don’t override it, the step will be completed immediately (by the successful Future
	 * returned)
	 *
	 * @param time The current simulation time in milliseconds
	 */
	override def stepUpdate(time: Int)(implicit exec: ExecutionContext) = Future.sequence(List(
		Future {
			val oldSpeed = speed
			if (acceleration < 0.0) {
				// only brake if no acceleration; also make sure we don’t end up with negative speeds
				// TODO introduce support for going backwards?
				speed = Math.max(0, speed + acceleration * (time - lastUpdateTime) / 1000)
				log.debug(f"Decreased speed: $oldSpeed%.2f -> $speed%.2f")
			} else {
				speed += acceleration * (time - lastUpdateTime) / 1000
				log.debug(f"Updated speed: $oldSpeed%.2f -> $speed%.2f")
			}
			if (speed > maxSpeed) {
				acceleration = 0.0
				speed = maxSpeed
			}
			travelledDistance += speed * (time - lastUpdateTime) / 1000

			lastUpdateTime = time
		},
		timer ? ScheduleStep(time + 10, self)
	))

}
