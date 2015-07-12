package info.andreaswolf.roadhopper.simulation

import akka.actor.{Actor, ActorRef}
import akka.pattern.ask
import akka.util.Timeout
import info.andreaswolf.roadhopper._

import scala.concurrent.duration._
import scala.collection.mutable.ListBuffer
import scala.concurrent.Future

class TwoStepSimulationTimer extends Actor {

	var time = 0
	var scheduledTime = 0

	val actors = new ListBuffer[ActorRef]()

	println("Creating timer")
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
		println("doUpdateStep()")
		require(time < scheduledTime, "Scheduled time must be in the future")
		time = scheduledTime

		println(f"======= Advancing time to $time")

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

			val actorFutures = new ListBuffer[Future[Any]]()
			actors.foreach(actor => {
				actorFutures.append(actor ? StepUpdate(time) andThen { case x => println("StepUpdate: Future finished") })
			})
			// wait for the result of the StepUpdate messages ...
			Future.sequence(actorFutures.toList).andThen({
				// ... and then run the StepAct messages
				case updateResult =>
					println("===== Update done, starting Act")
					actorFutures.clear()
					actors.foreach(actor => {
						actorFutures.append(actor ? StepAct(time) andThen { case x => println("StepAct: Future finished") })
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

		case Simulate() =>
			start()

		/**
		 * Used by actors to schedule their invocation at some point in the future.
		 */
		case ScheduleRequest(time: Int) =>
			println(f"Scheduling for $time by ${sender().path}")
			this.scheduledTime = time
			sender ! true

		case Pass() => println("Pass()")
	}

}
