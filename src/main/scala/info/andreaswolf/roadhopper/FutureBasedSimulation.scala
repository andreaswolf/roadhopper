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
				Thread.sleep(20)
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
 * See https://stackoverflow.com/a/8683439/3987705
 */
trait SimulationActor extends Actor {
	implicit val timeout = Timeout(60 seconds)
	import context.dispatcher

	var _receive : List[Receive] = List(
		{
			case StepUpdate(time) =>
				val originalSender = sender()
				doUpdate(time) andThen {
					case x =>
						println(f"StepUpdate done for " + self.path)
						originalSender ! true
				}

			case StepAct(time) =>
				val originalSender = sender()
				doAct(time) andThen {
					case x =>
						println(f"StepUpdate done for " + self.path)
						originalSender ! true
				}
		}
	)
	def receiver(receive: Actor.Receive) { _receive = receive :: _receive }
	def receive =  _receive reduce {_ orElse _}

	def doUpdate(time: Int)(implicit exec: ExecutionContext): Future[Any]

	def doAct(time: Int)(implicit exec: ExecutionContext): Future[Any]
}

class ExtensionComponent extends SimulationActor {
	receiver({
		case Start() =>
			println("starting")
			sender ! true
	});

	override def doUpdate(time: Int)(implicit exec: ExecutionContext): Future[Any] = {
		Future {
			println("foo")
		}
	}

	override def doAct(time: Int)(implicit exec: ExecutionContext): Future[Any] = {
		Future {
			println("bar")
		}
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
			Thread.sleep(50)
			println("Component: Updating time to " + time)
			sender ! true

		case StepAct(time) =>
			println("Component::StepAct")
			Thread.sleep(2000)
			println("Component: Got time " + time)
			val originalSender = sender()
			Future sequence List(
				// ask another component a question
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
			// we cannot use sender, as a message passed to the sender will be treated as the acknowledgement of the ask()
			// request.
			sender ! Answer(time)
			println(f"Subordinate: $time")

	}
}
