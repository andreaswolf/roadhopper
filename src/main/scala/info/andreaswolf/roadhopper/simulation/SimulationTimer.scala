package info.andreaswolf.roadhopper.simulation

import akka.actor.{Actor, ActorRef}

import scala.collection.mutable

case class Step(time: Int)
case class StartSimulation(actors: List[ActorRef])
case class Start()
case class ScheduleRequest(time: Int)
case class Pass()

class SimulationTimerActor extends Actor {
	var currentTime: Int = 0

	var started = false

	var calledActors = List[ActorRef]()

	val scheduledTimes = scala.collection.mutable.HashMap.empty[ActorRef, Int]

	override def receive: Receive = {
		case StartSimulation(actors) =>
			println("Started simulation")

			started = true
			calledActors = actors
			actors.foreach((actor) => {
				actor ! Start()
			})

		case ScheduleRequest(time) =>
			println(calledActors.size + " actors still called; scheduling " + sender().path + " for " + time)
			scheduledTimes.update(sender(), time)
			unscheduleActor(sender())

		case Pass() =>
			unscheduleActor(sender())
	}

	private def unscheduleActor(actor: ActorRef): Unit = {
		calledActors = calledActors.filter(_ != actor)
		if (started && calledActors.isEmpty) {
			advance()
		}
	}

	private def advance(): Unit = {
		if (scheduledTimes.isEmpty) {
			println("No more actors scheduled => simulation can end")
			return
		}
		require(currentTime < scheduledTimes.values.min, "Next time must be after current time")
		// find next scheduled point in time
		currentTime = scheduledTimes.values.min

		println("Advancing to time " + currentTime)

		// TODO this works for now, but we should publish messages to the bus instead and unset the scheduled time
		// The scheduledTimes map should also be time => List[ActorRef] instead
		val scheduledActors: mutable.HashMap[ActorRef, Int] = scheduledTimes.filter(_._2 == currentTime)
		if (scheduledActors.isEmpty) {
			println("Nothing scheduled")
		}

		calledActors = scheduledActors.keys.toList
		scheduledActors.foreach(t => {
			val (actor: ActorRef, _) = t
			println("Calling actor " + actor.path)
			// make sure this actor is not scheduled unless they schedule themselves again
			scheduledTimes.remove(actor)
			println(scheduledTimes.size + " actors scheduled")
			actor ! Step(currentTime)
		})
		println(calledActors.size + " actors called")
	}

}
