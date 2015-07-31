package info.andreaswolf.roadhopper.simulation.signals

import akka.actor.ActorRef
import akka.pattern.ask
import info.andreaswolf.roadhopper.simulation.{ScheduleStep, SimulationActor}
import info.andreaswolf.roadhopper.simulation.signals.Process.Invoke

import scala.annotation.tailrec
import scala.collection.mutable
import scala.collection.mutable.ListBuffer
import scala.concurrent.{Promise, Future, ExecutionContext}
import scala.util.Try


object SignalBus {

	case class DefineSignal(signalName: String)

	case class UpdateSignalValue(signalName: String, newValue: AnyVal)

	case class SubscribeToSignal(signalName: String, subscriber: ActorRef)

}


/**
 * An implementation of the concept of "signals" as the output of components.
 *
 * One possible signal that components can listen to is the time, which will lead to
 *
 * Signals are declared by a component, other components can subscribe to them (= get notified when the signal changes).
 * With every new time step, an initial update is triggered with the components that listen to the time.
 * TODO add support for scheduling signal updates at a future time
 *
 * Every component can have one or more so-called processes that react to updates of one or more signals. With every
 * update of these signals, the process is triggered.
 */
class SignalBus(val timer: ActorRef) extends SimulationActor {

	import SignalBus._
	import context.dispatcher

	/**
	 * All signals that were defined.
	 */
	val definedSignals: mutable.MutableList[String] = mutable.MutableList(
		"time"
	)

	val subscribers: mutable.HashMap[String, ListBuffer[ActorRef]] = new mutable.HashMap[String, ListBuffer[ActorRef]]()

	val scheduledUpdates: mutable.HashMap[String, AnyVal] = new mutable.HashMap[String, AnyVal]()

	val currentTimeStepPromise: Promise[Any] = Promise.apply[Any]()

	registerReceiver {
		case DefineSignal(name) =>
			if (!definedSignals.contains(name)) {
				definedSignals += name

				sender() ! true
			} else {
				sender() ! "Signal already registered"
			}

		case SubscribeToSignal(signal, subscriber) =>
			val existingSubscriptions = subscribers.getOrElseUpdate(signal, new ListBuffer[ActorRef]())

			if (!existingSubscriptions.contains(subscriber)) {
				existingSubscriptions append subscriber
			}

		case UpdateSignalValue(name, value) =>
			scheduledUpdates.put(name, value)

			sender() ! true

	}

	/**
	 * Handler for [[info.andreaswolf.roadhopper.simulation.Start]] messages.
	 * <p/>
	 * The simulation will only continue after the Future has been completed.
	 */
	override def start()(implicit exec: ExecutionContext): Future[Any] = timer ? ScheduleStep(10, self)

	/**
	 * Handler for [[info.andreaswolf.roadhopper.simulation.StepUpdate]] messages.
	 * <p/>
	 * The simulation will only continue after the Future has been completed.
	 */
	override def stepUpdate()(implicit exec: ExecutionContext): Future[Any] = {
		{
			scheduledUpdates.put("time", time)
			runDeltaCycle(1)
			// This future is completed by runDeltaCycle() once no further value updates were scheduled
			Future.sequence(List(
				currentTimeStepPromise.future,
				timer ? ScheduleStep(time + 10, self)
			))
		}
	}

	/**
	 * Runs a single delta cycle and afterwards recursively invokes the next cycle as long as updates were scheduled.
	 * If no updates are scheduled, the promise for the current time step is marked as fulfilled.
	 */
	protected def runDeltaCycle(cycles: Int = 0): Future[Any] = {
		if (scheduledUpdates.isEmpty) {
			log.info(s"Finished time step after ${cycles-1} delta cycles. ")
			currentTimeStepPromise.complete(Try(true))
			return Future.successful()
		}

		{
			val futures = ListBuffer[Future[Any]]()
			scheduledUpdates.foreach[Unit] { case(name, value) =>
				futures append updateValue(name, value)
			}

			scheduledUpdates.clear()

			Future.sequence(futures.toList)
		} flatMap { x =>
			// TODO can we make this non-recursive?
			runDeltaCycle(cycles + 1)
		}
	}

	/**
	 * Invokes all subscribers for the given signal name
	 */
	protected def updateValue(signalName: String, signalValue: AnyVal): Future[Any] = {
		def invokeSubscriber(subscriber: ActorRef): Future[Any] = {
			subscriber ? Invoke(signalName)
		}
		val subscriberFutures: List[Future[Any]] = (for (subscriber <- subscribers.getOrElse(signalName, new ListBuffer[ActorRef]())) yield {
			invokeSubscriber(subscriber)
		}).toList
		log.info(s"Informed ${subscriberFutures.length} subscribers about a change of signal $signalName")
		Future.sequence(subscriberFutures)
	}
}
