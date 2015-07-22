package info.andreaswolf.roadhopper.simulation

import akka.actor.{ActorLogging, ActorRef, Actor}
import akka.util.Timeout

import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}


case class Start()

case class StepUpdate(time: Int)

case class StepAct(time: Int)


/**
 * This is an extensible simulation actor that holds standard behaviour common to all timed simulation actors:
 * it reacts to [[Start]], [[StepUpdate]] and [[StepAct]] messages and makes that responses to the messages are not sent
 * before all processing has been done.
 * <p/>
 * Processing of messages is delegated to handler functions that can (and should) be overridden in classes using this
 * trait.
 * <p/>
 * See https://stackoverflow.com/a/8683439/3987705 for the inspiration for this trait.
 */
trait SimulationActor extends Actor with ActorLogging {
	implicit val timeout = Timeout(60 seconds)
	import context.dispatcher

	/**
	 * The list of message handlers. By default, it contains handlers for the basic simulation messages Start(),
	 * StepUpdate() and StepAct()
	 * <p/>
	 * See [[registerReceiver()]] for more information on how to add your own handlers.
	 * <p/>
	 * WARNING: it is undefined in which order the case statements from the different Receive instances will be invoked
	 * (as the list is not ordered). If we need to explicitly override any of the cases defined here, we need to convert
	 * this List() into something with explicit ordering.
	 */
	var _receive : List[Receive] = List(
		{
			// TODO Make a method that creates a ListBuffer of Futures and turns them into a sequence that is then checked by the individual methods in here
			case Start() =>
				// we need to store sender() here as sender() will point to the dead letter mailbox when andThen() is called.
				// TODO find out why this is the case
				val originalSender = sender()
				start() andThen {
					case x =>
						originalSender ! true
				}

			case StepUpdate(time) =>
				val originalSender = sender()
				stepUpdate(time) andThen {
					case x =>
						originalSender ! true
				}

			case StepAct(time) =>
				val originalSender = sender()
				stepAct(time) andThen {
					case x =>
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
	 * <p/>
	 * WARNING the execution order of the receive functions is currently undefined. If you need to override an existing
	 *         message handler, make sure to fix this issue first!
	 */
	def registerReceiver(receive: Actor.Receive) { _receive = receive :: _receive }

	/**
	 * The receive function, must not be overridden. Instead, register your own receiver function with
	 * [[registerReceiver()]]
	 */
	final def receive =  _receive reduce {_ orElse _}

	/**
	 * Handler for [[Start]] messages.
	 * <p/>
	 * The simulation will only continue after the Future has been completed. You can, but don’t need to override this
	 * method in your actor. If you don’t override it, the step will be completed immediately (by the successful Future
	 * returned)
	 */
	def start()(implicit exec: ExecutionContext): Future[Any] = Future.successful()

	/**
	 * Handler for [[StepUpdate]] messages.
	 * <p/>
	 * The simulation will only continue after the Future has been completed. You can, but don’t need to override this
	 * method in your actor. If you don’t override it, the step will be completed immediately (by the successful Future
	 * returned)
	 *
	 * @param time The current simulation time in milliseconds
	 */
	def stepUpdate(time: Int)(implicit exec: ExecutionContext): Future[Any] = Future.successful()

	/**
	 * Handler for [[StepAct]] messages.
	 * <p/>
	 * The simulation will only continue after the Future has been completed. You can, but don’t need to override this
	 * method in your actor. If you don’t override it, the step will be completed immediately (by the successful Future
	 * returned)
	 *
	 * @param time The current simulation time in milliseconds
	 */
	def stepAct(time: Int)(implicit exec: ExecutionContext): Future[Any] = Future.successful()
}
