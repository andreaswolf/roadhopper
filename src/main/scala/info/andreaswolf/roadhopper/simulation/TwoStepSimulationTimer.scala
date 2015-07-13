package info.andreaswolf.roadhopper.simulation

import akka.actor.{ActorLogging, Actor, ActorRef}
import akka.pattern.ask
import akka.util.Timeout

import scala.collection.mutable
import scala.concurrent.duration._
import scala.collection.mutable.ListBuffer
import scala.concurrent.Future

case class ScheduleStep(time: Int, actor: ActorRef)

/**
 * A simulation timer that breaks each step into two phases: an update and an act phase. In the update phase, each
 * component should update its internal state, however it might have changed since the last step.
 * In the act phase, the components can ask each other for their state and react to state changes, e.g. by adjusting
 * their state and future behaviour.
 */
class TwoStepSimulationTimer extends Actor {

	var currentTime = 0

	val actors = new ListBuffer[ActorRef]()

	val timeSchedule = new mutable.HashMap[Int, ListBuffer[ActorRef]]()

	implicit val timeout = Timeout(60 seconds)

	/**
	 * Starts the simulation by issuing a Start() message to all registered actors and waiting for their response.
	 */
	def start() = {
		import context.dispatcher

		println("Starting")

		// initialize all actors by sending them a Start() message and wait for all results
		val actorFutures = new ListBuffer[Future[Any]]()
		actors.foreach(actor => {
			actorFutures.append(actor ? Start())
		})
		Future.sequence(actorFutures.toList).andThen({
			// let the loop roll
			case x =>
				println("starting loop")
				doStep()
		})
		println("start(): done")
	}

	/**
	 * Implements a two-step scheduling process: first an UpdateStep() is sent to all scheduled actors, then
	 * an ActStep is sent.
	 *
	 * For each step, the result of all messages is awaited before continuing, making the simulation run with proper
	 * ordering.
	 */
	def doStep(): Unit = {
		println("doStep()")

		val nextTime = timeSchedule.keys.min
		require(currentTime < nextTime, "Scheduled time must be in the future")
		currentTime = nextTime

		println(f"======= Advancing time to $currentTime")

		/**
		 * The main method responsible for performing a step:
		 *
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
				actorFutures.append(actor ? StepUpdate(currentTime) andThen { case x => println("StepUpdate: Future finished") })
			})
			// wait for the result of the StepUpdate messages ...
			Future.sequence(actorFutures.toList).andThen({
				// ... and then run the StepAct messages
				case updateResult =>
					println("===== Update done, starting Act")
					actorFutures.clear()
					actorsToCall.foreach(actor => {
						actorFutures.append(actor ? StepAct(currentTime) andThen { case x => println("StepAct: Future finished") })
					})
					// wait for the result of the StepAct messages
					// TODO properly check for an error here -> transform this block to this:
					//   andThen{ case Success(x) => … case Failure(x) => }
					Future.sequence(actorFutures.toList).onSuccess({
						case actResult =>
							this.doStep()
					})
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

		/**
		 * Used by actors to schedule their invocation at some point in the future.
		 */
		case ScheduleStep(time: Int, target: ActorRef) =>
			val alreadyScheduled = timeSchedule.getOrElseUpdate(time, new ListBuffer[ActorRef]())
			alreadyScheduled append target
			sender ! true

		case Pass() => println("Pass()")
	}

}
