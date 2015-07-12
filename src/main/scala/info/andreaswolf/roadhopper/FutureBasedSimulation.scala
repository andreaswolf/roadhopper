package info.andreaswolf.roadhopper

/**
 * This file is a playground to test new ideas for the simulation without the hassle of firing up a complete
 * GraphHopper instance etc.
 */

import akka.actor.{ActorRef, Props, ActorSystem, Actor}
import akka.pattern.ask
import akka.util.Timeout

import scala.collection.mutable.ListBuffer
import scala.concurrent.{ExecutionContext, Await, Future}
import scala.concurrent.duration._
import scala.util.Success

/**
 * This simulation coordinates the single steps with the help of futures, requiring every actor to explicitly respond
 * to a Step() message. Only after this response has been received from all actors, the current step is regarded as
 * being completed.
 */
object FutureBasedSimulation extends App {
	val actorSystem = ActorSystem.create("futures")

	val timer = actorSystem.actorOf(Props(new Timer), "timer")

	val component = actorSystem.actorOf(Props(new Component(timer)), "component")
	val extensionComponent = actorSystem.actorOf(Props(new ExtensionComponent), "extension")

	implicit val timeout = Timeout(10 seconds)
	timer ? RegisterActor(component)
	timer ? RegisterActor(extensionComponent)
	timer ! Simulate()
}

case class Simulate()
case class Start()
case class RegisterActor(actor: ActorRef)
case class StepUpdate(time: Int)
case class StepAct(time: Int)
case class ScheduleRequest(time: Int)
case class Pass()

class Timer extends Actor {

	var time = 0
	var scheduledTime = 0

	val actors = new ListBuffer[ActorRef]()

	println("Creating timer")
	implicit val timeout = Timeout(60 seconds)

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
		implicit val timeout = Timeout(60 seconds)
		import context.dispatcher

		println("doUpdateStep()")
		require(time < scheduledTime, "Scheduled time must be in the future")
		time = scheduledTime

		println(f"======= Advancing time to $time")
		val actorFutures = new ListBuffer[Future[Any]]()
		actors.foreach(actor => {
			actorFutures.append(actor ? StepUpdate(time) andThen {case x => println("StepUpdate: Future finished")})
		})
		// wait for the result of the StepUpdate messages ...
		Future.sequence(actorFutures.toList).andThen({
			// ... and then run the StepAct messages
			case updateResult =>
				println("===== Update done, starting Act")
				actorFutures.clear()
				actors.foreach(actor => {
					actorFutures.append(actor ? StepAct(time) andThen {case x => println("StepAct: Future finished")})
				})
				// wait for the result of the StepAct messages
				// TODO properly check for an error here -> transform this block to this:
				//   andThen{ case Success(x) => … case Failure(x) => }
				Future.sequence(actorFutures.toList).onSuccess({
					case actResult if time < 1000 => this.doStep()
					case actResult => context.system.shutdown()
				})
		})
	}

	def receive = {
		case RegisterActor(actor) =>
			actors append actor

		case Simulate() =>
			start()

		case ScheduleRequest(time: Int) =>
			println(f"Scheduling for $time by ${sender().path}")
			this.scheduledTime = time
			sender ! true

		case Pass() => println("Pass()")
	}

}

/**
 * This is an extensible simulation actor that holds standard behaviour common to all timed simulation actors:
 * it reacts to Start(), StepUpdate() and StepAct() messages and makes that responses to the messages are not sent
 * before all processing has been done.
 *
 * Processing of messages is delegated to handler functions that can (and should) be overridden in classes using this
 * trait.
 *
 * See https://stackoverflow.com/a/8683439/3987705
 */
trait SimulationActor extends Actor {
	implicit val timeout = Timeout(60 seconds)
	import context.dispatcher

	// WARNING: it is undefined in which order the case statements from the different Receive instances will be invoked
	// (as the list is not ordered). If we need to explicitly override any of the cases defined here, we need to convert
	// this List() into something with explicit ordering.
	var _receive : List[Receive] = List(
		{
			case Start() =>
				// we need to store sender() here as sender() will point to the dead letter mailbox when andThen() is called.
				// TODO find out why this is the case
				val originalSender = sender()
				start() andThen {
					case x =>
						println("Start done for " + self.path)
						println(sender())
						println(originalSender)
						originalSender ! true
				}

			case StepUpdate(time) =>
				val originalSender = sender()
				stepUpdate(time) andThen {
					case x =>
						println(f"StepUpdate done for " + self.path)
						originalSender ! true
				}

			case StepAct(time) =>
				val originalSender = sender()
				stepAct(time) andThen {
					case x =>
						println(f"StepAct done for " + self.path)
						originalSender ! true
				}
		}
	)

	/**
	 * Registers a new receiver. Call with a partial function to make the actor accept additional types of messages.
	 * <p/>
	 * Example:
	 * <p/>
	 * <pre>
	 * registerReceiver {
	 *   case MyMessage() =>
	 *     // code to handle MyMessage()
	 * }</pre>
	 *
	 * WARNING the execution order of the receive functions is currently undefined. If you need to override an existing
	 *         message handler, make sure to fix this issue first!
	 */
	def registerReceiver(receive: Actor.Receive) { _receive = receive :: _receive }
	def receive =  _receive reduce {_ orElse _}

	/**
	 * Handler for Start() messages.
	 *
	 * The simulation will only continue after the Future has been completed. You can, but don’t need to override this
	 * method in your actor. If you don’t override it, the step will be completed immediately (by the successful Future
	 * returned)
	 */
	def start()(implicit exec: ExecutionContext): Future[Any] = Future.successful()

	/**
	 * Handler for StepUpdate() messages.
	 *
	 * The simulation will only continue after the Future has been completed. You can, but don’t need to override this
	 * method in your actor. If you don’t override it, the step will be completed immediately (by the successful Future
	 * returned)
	 *
	 * @param time The current simulation time in milliseconds
	 */
	def stepUpdate(time: Int)(implicit exec: ExecutionContext): Future[Any] = Future.successful()

	/**
	 * Handler for StepAct() messages.
	 *
	 * The simulation will only continue after the Future has been completed. You can, but don’t need to override this
	 * method in your actor. If you don’t override it, the step will be completed immediately (by the successful Future
	 * returned)
	 *
	 * @param time The current simulation time in milliseconds
	 */
	def stepAct(time: Int)(implicit exec: ExecutionContext): Future[Any] = Future.successful()
}

class ExtensionComponent extends SimulationActor {

	override def stepUpdate(time: Int)(implicit exec: ExecutionContext): Future[Any] = Future {
		println("foo")
	}

	override def stepAct(time: Int)(implicit exec: ExecutionContext): Future[Any] = Future {
		println("bar")
	}
}

class AnotherComponent extends Actor {

	var foo = 0

	def receive = {
		case StepUpdate(time) =>
			println("AnotherComponent::StepUpdate")
			Thread.sleep(100)
			foo += 1
			sender ! true

		case StepAct(time) =>
			println("AnotherComponent::StepAct")
			Thread.sleep(150)
			println(f"foo: $foo")
			sender ! true

	}
}

class Component(val timer: ActorRef) extends Actor {

	implicit val timeout = Timeout(10 seconds)
	import context.dispatcher

	val subordinate = context.actorOf(Props(new Subordinate(self)), "subordinate")

	def receive = {
		case Start() =>
			val originalSender = sender()
			timer ? ScheduleRequest(50) andThen { case x =>
				println("Component::Start() finished")
				originalSender ! true
			}

		case StepUpdate(time) =>
			println("Component::StepUpdate")
			println("Sleeping 1000ms…")
			Thread.sleep(1000)
			println("1000ms done")
			println("Component: Updating time to " + time)
			sender ! true

		case StepAct(time) =>
			println("Component::StepAct")
			println("Sleeping 500…")
			Thread.sleep(500)
			println("500ms done")
			println("Component: Got time " + time)
			val originalSender = sender()
			Future sequence List(
				// ask another component a question => we need to directly handle the result inline; this could also be moved
				// to a method, but we cannot use this actor’s receive method, otherwise we cannot use sender() to send the
				// response in subordinate.
				subordinate ? Question(time) andThen {
					case Success(Answer(x)) =>
						println("Answer for " + time)
					case Success(x) => println(x)
				},
				timer ? ScheduleRequest(time + 100)
			) andThen {
				case x =>
					println(f"Scheduling request of ${self.path} passed")
					originalSender ! true
			}

		case Answer(time) =>
			println("Answer for " + time)
			sender ! true
	}
}

case class Question(time: Int)
case class Answer(time: Int)

class Subordinate(val component: ActorRef) extends Actor {
	implicit val timeout = Timeout(10 seconds)

	def receive = {
		case Question(time) =>
			println(f"Subordinate: $time")
			sender ! Answer(time)

	}
}
