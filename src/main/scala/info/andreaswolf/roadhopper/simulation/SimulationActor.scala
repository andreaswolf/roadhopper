package info.andreaswolf.roadhopper.simulation

import akka.actor.{ActorLogging, ActorRef, Actor}
import akka.util.Timeout

import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}


/** Message sent at the beginning of a time step to propagate the new time throughout the system. */
case class TellTime(time: Int)

/** Tell an actor that the simulation has started */
case class Start()

/** First of the two phases of a simulation step */
case class StepUpdate()

/** Second of the two phases of a simulation step */
case class StepAct()


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
trait SimulationActor extends Actor with ActorLogging with ExtensibleReceiver {
	implicit val timeout = Timeout(60 seconds)
	import context.dispatcher

	/**
	 * The current time in milliseconds
	 */
	var time: Int = 0

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
	registerReceiver(
		{
			case TellTime(currentTime) =>
				val oldTime = time
				val originalSender = sender()
				time = currentTime
				timeAdvanced(oldTime, currentTime) andThen {
					case x =>
						originalSender ! true
				}

			case Start() =>
				// we need to store sender() here as the sender reference is lost once this method was executed (and therefore
				// also in the asynchronously executed andThen {} block).
				val originalSender = sender()
				start() andThen {
					case x =>
						originalSender ! true
				}

			case StepUpdate() =>
				val originalSender = sender()
				stepUpdate() andThen {
					case x =>
						originalSender ! true
				}

			case StepAct() =>
				val originalSender = sender()
				stepAct() andThen {
					case x =>
						originalSender ! true
				}
		}
	)


	def timeAdvanced(oldTime: Int, newTime: Int ): Future[Any] = Future.successful()

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
	 */
	def stepUpdate()(implicit exec: ExecutionContext): Future[Any] = Future.successful()

	/**
	 * Handler for [[StepAct]] messages.
	 * <p/>
	 * The simulation will only continue after the Future has been completed. You can, but don’t need to override this
	 * method in your actor. If you don’t override it, the step will be completed immediately (by the successful Future
	 * returned)
	 */
	def stepAct()(implicit exec: ExecutionContext): Future[Any] = Future.successful()
}
