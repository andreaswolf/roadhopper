package info.andreaswolf.roadhopper.simulation

import akka.actor.{Actor, ActorRef}

import scala.collection.mutable

case class Step(time: Int)
case class Start()
case class ScheduleRequest(time: Int)

class SimulationTimerActor extends Actor {
	var currentTime: Int = 0

	val scheduledTimes = scala.collection.mutable.HashMap.empty[ActorRef, Int]

	override def receive: Receive = {
		case Start() =>
			println("Started simulation")

			ActorBasedSimulation.timeBus.publish(MsgEnvelope("time.step", new Step(currentTime)))

		case ScheduleRequest(time) =>
			scheduledTimes.update(sender(), time)
			if (scheduledTimes.count(_._2 == currentTime) == 0) {
				advance()
			}
	}

	private def advance(): Unit = {
		// find next scheduled point in time
		val (_, currentTime) = scheduledTimes.reduce((x, y) => if (x._2 < y._2) x else y)

		// TODO this works for now, but we should publish messages to the bus instead and unset the scheduled time
		// The scheduledTimes map should also be time => List[ActorRef] instead
		val scheduledActors: mutable.HashMap[ActorRef, Int] = scheduledTimes.filter(_._2 == currentTime)
		if (scheduledActors.isEmpty) {
			println("Nothing scheduled")
		}
		scheduledActors.foreach(t => {
			val (actor: ActorRef, time: Int) = t
			// make sure this actor is not scheduled unless they schedule themselves again
			scheduledTimes.remove(actor)
			actor ! new Step(currentTime)
		})
	}

}
