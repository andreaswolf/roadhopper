package info.andreaswolf.roadhopper.simulation

import akka.actor.{ActorLogging, Actor, ActorRef}
import akka.pattern.ask
import akka.util.Timeout

import scala.collection.mutable
import scala.concurrent.duration._
import scala.collection.mutable.ListBuffer
import scala.concurrent.Future

/**
 * Message sent to start the simulation
 */
case class StartSimulation()

/**
 * Message sent to stop the timer
 */
case class Stop()

case class RegisterActor(actor: ActorRef)

case class ScheduleStep(time: Int, actor: ActorRef)


/**
 * A simulation timer that breaks each step into two phases: an update and an act phase. In the update phase, each
 * component should update its internal state, however it might have changed since the last step.
 * In the act phase, the components can ask each other for their state and react to state changes, e.g. by adjusting
 * their state and future behaviour.
 */
class TwoStepSimulationTimer extends Actor with ActorLogging {

	var currentTime = 0

	var running = false

	val actors = new ListBuffer[ActorRef]()

	val timeSchedule = new mutable.HashMap[Int, ListBuffer[ActorRef]]()

	implicit val timeout = Timeout(60 seconds)

	/**
	 * Starts the simulation by issuing a Start() message to all registered actors and waiting for their response.
	 */
	def start() = {
		import context.dispatcher

		running = true

		// initialize all actors by sending them a Start() message and wait for all results
		val actorFutures = new ListBuffer[Future[Any]]()
		actors.foreach(actor => {
			actorFutures.append(actor ? Start())
		})
		Future.sequence(actorFutures.toList).andThen({
			// let the loop roll
			case x =>
				doStep()
		})
	}

	/**
	 * Implements a two-step scheduling process: first an UpdateStep() is sent to all scheduled actors, then
	 * an ActStep is sent.
	 * <p/>
	 * For each step, the result of all messages is awaited before continuing, making the simulation run with proper
	 * ordering.
	 */
	def doStep(): Unit = {
		val nextTime = timeSchedule.keys.min
		require(currentTime < nextTime, "Scheduled time must be in the future")
		currentTime = nextTime

		/**
		 * The main method responsible for performing a step:
		 * <p/>
		 * First sends a [[StepUpdate]] to every actor, waits for their results and then sends [[StepAct]] to every actor.
		 * For each of these two steps, one [[Future]] is constructed with [[Future.sequence()]] that holds all the
		 * message [[Future]]s.
		 */
		def callActors(): Unit = {
			implicit val timeout = Timeout(60 seconds)
			import context.dispatcher

			// we can be sure that there is a list, thus we can use .get
			val actorsToCall = timeSchedule.remove(nextTime).get.distinct

			val actorFutures = new ListBuffer[Future[Any]]()
			actorsToCall.foreach(actor => {
				actorFutures.append(actor ? StepUpdate(currentTime))
			})
			// wait for the result of the StepUpdate messages ...
			Future.sequence(actorFutures.toList).andThen({
				// ... and then run the StepAct messages
				case updateResult =>
					if (running) {
						// if the simulation ended during the update step, continuing here would raise an exception.
						// doing a return here led to scala.runtime.NonLocalReturnControl$mcV$sp errors
						// TODO find a way to only schedule the simulation end during the update step and continue until after the
						// next step

						actorFutures.clear()
						actorsToCall.foreach(actor => {
							actorFutures.append(actor ? StepAct(currentTime))
						})
						// wait for the result of the StepAct messages
						// TODO properly check for an error here -> transform this block to this:
						//   andThen{ case Success(x) => … case Failure(x) => }
						Future.sequence(actorFutures.toList).onSuccess({
							case actResult =>
								if (running) {
									this.doStep()
								}
						})
					}
			})
		}

		callActors()
	}

	def receive = {
		case RegisterActor(actor) =>
			actors append actor
			sender ! true

		case StartSimulation() =>
			start()

		case Stop() =>
			log.info(s"Stopping timer at $currentTime")
			running = false
			context.system.shutdown()

		/**
		 * Used by actors to schedule their invocation at some point in the future.
		 */
		case ScheduleStep(time: Int, target: ActorRef) =>
			val alreadyScheduled = timeSchedule.getOrElseUpdate(time, new ListBuffer[ActorRef]())
			alreadyScheduled append target
			sender ! true
	}

}
