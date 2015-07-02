package info.andreaswolf.roadhopper.simulation

import akka.actor.{ActorLogging, PoisonPill, Actor, ActorRef}

import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer

case class Step(time: Int)
case class StartSimulation()
case class RegisterActors(actors: List[ActorRef])
case class RegisterActor(actor: ActorRef)
case class ActorsRegistered()
case class StopSimulation()
case class Start()
case class ScheduleRequest(time: Int)
case class Pass()

class SimulationTimerActor extends Actor with ActorLogging {
	var currentTime: Int = 0

	var started = false

	var calledActors: List[ActorRef] = List[ActorRef]()
	var registeredActors: mutable.Buffer[ActorRef] = ArrayBuffer[ActorRef]()

	val scheduledTimes = scala.collection.mutable.HashMap.empty[ActorRef, Int]

	override def receive: Receive = {
		/**
		 * Starts the simulation. The list of actors is required because the timer needs to know for which components to
		 * wait
		 */
		case StartSimulation() =>
			log.debug("Starting simulation")

			started = true
			calledActors = registeredActors.toList
			calledActors.foreach((actor) => {
				actor ! Start()
			})

		case RegisterActors(actors) =>
			registeredActors ++= actors
			sender ! ActorsRegistered()

		case RegisterActor(actor) =>
			if (started) {
				// TODO allow this; precondition is that we can get the actors to speed after they were added → decouple actor
				// registration and system start
				log.error("Simulation already started; cannot register actor anymore!")
			}
			log.debug("Registering actor " + actor)
			registeredActors append actor

		case StopSimulation() =>
			if (started) {
				log.info("Stopping simulation")
				started = false
				registeredActors.foreach((actor) => {
					actor ! PoisonPill
				})
			}

		case ScheduleRequest(time) =>
			require(currentTime < time, "Can only schedule for the future")
			//println(calledActors.size + " actors still called; scheduling " + sender().path + " for " + time)
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

		// TODO this works for now, but we should publish messages to the bus instead and unset the scheduled time
		// The scheduledTimes map should also be time => List[ActorRef] instead
		val scheduledActors: mutable.HashMap[ActorRef, Int] = scheduledTimes.filter(_._2 == currentTime)
		if (scheduledActors.isEmpty) {
			println("Nothing scheduled")
		}

		calledActors = scheduledActors.keys.toList
		scheduledActors.foreach(t => {
			val (actor: ActorRef, _) = t
			//println("Calling actor " + actor.path)
			// make sure this actor is not scheduled unless they schedule themselves again
			scheduledTimes.remove(actor)
			//println(scheduledTimes.size + " actors scheduled")
			actor ! Step(currentTime)
		})
		//println(calledActors.size + " actors called")
	}

}
