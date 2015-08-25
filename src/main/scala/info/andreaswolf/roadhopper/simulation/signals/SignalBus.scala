package info.andreaswolf.roadhopper.simulation.signals

import akka.actor.ActorRef
import akka.pattern.ask
import info.andreaswolf.roadhopper.simulation.{TellTime, ScheduleStep, SimulationActor}
import info.andreaswolf.roadhopper.simulation.signals.Process.Invoke

import scala.collection.mutable
import scala.collection.mutable.ListBuffer
import scala.concurrent.{Promise, Future, ExecutionContext}
import scala.util.Try


object SignalBus {

	case class DefineSignal(signalName: String)

	/**
	 * @param delta The time (from now on) when the signal should be updated
	 */
	case class ScheduleSignalUpdate(delta: Int, signalName: String, newValue: Any)

	case class UpdateSignalValue(signalName: String, newValue: Any)

	case class SubscribeToSignal(signalName: String, subscriber: ActorRef)

}


/**
 * An implementation of the concept of "signals" as the output of components.
 *
 * One possible signal that components can listen to is the time, which will lead to
 *
 * Signals are declared by a component, other components can subscribe to them (= get notified when the signal changes).
 * With every new time step, an initial update is triggered with the components that listen to the time.
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

	/**
	 * The current signal state.
	 *
	 * TODO merge this with definedSignals
	 */
	var signals = new SignalState()

	val subscribers: mutable.HashMap[String, ListBuffer[ActorRef]] = new mutable.HashMap[String, ListBuffer[ActorRef]]()

	val scheduledUpdates: mutable.HashMap[String, Any] = new mutable.HashMap[String, Any]()

	val futureScheduledUpdates: mutable.HashMap[Int, mutable.HashMap[String, Any]] = new mutable.HashMap[Int, mutable.HashMap[String, Any]]()

	var currentTimeStepPromise: Promise[Any] = Promise.apply[Any]()


	registerReceiver {
		case DefineSignal(name) =>
			if (!definedSignals.contains(name)) {
				definedSignals += name
				// TODO initialize signal in SignalState

				sender() ! true
			} else {
				sender() ! "Signal already registered"
			}

		case SubscribeToSignal(signal, subscriber) =>
			val existingSubscriptions = subscribers.getOrElseUpdate(signal, new ListBuffer[ActorRef]())

			if (!existingSubscriptions.contains(subscriber)) {
				existingSubscriptions append subscriber
			}
			sender() ! true

		case UpdateSignalValue(name, value) =>
			scheduledUpdates.put(name, value)

			sender() ! true

		case ScheduleSignalUpdate(delta, name, value) =>
			val updatesForTime = futureScheduledUpdates.getOrElseUpdate(time + delta, new mutable.HashMap[String, Any]())
			// TODO check if there already is an update scheduled => how to react then?
			updatesForTime.put(name, value)

			sender() ! true

	}


	override def timeAdvanced(oldTime: Int, newTime: Int): Future[Any] = {
		val actorsToInform = subscribers.flatMap(subscription => subscription._2).toList.distinct
		Future.sequence(actorsToInform.map(actor => actor ? TellTime(newTime)).toList)
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
			currentTimeStepPromise = Promise.apply[Any]()
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
	protected def runDeltaCycle(cycle: Int = 1): Future[Any] = {
		if (scheduledUpdates.isEmpty && cycle > 1) {
			log.info(s"Finished time step $time after ${cycle - 1} delta cycles. ")
			currentTimeStepPromise.complete(Try(true))
			return Future.successful()
		}

		{
			if (cycle == 1) {
				assert(scheduledUpdates.isEmpty, "ERROR: Regular updates scheduled for first delta cycle!")
				scheduledUpdates.clear()
				scheduledUpdates ++= futureScheduledUpdates.remove(time).getOrElse(new mutable.HashMap[String, AnyVal]())

				scheduledUpdates.put("time", time)
			}
			signals = new SignalState(scheduledUpdates, signals)

			val updatedSignals = scheduledUpdates.keys.toList
			// get a list of all subscribers we must notify (by the signals that were updated) and then make sure that
			// each is only called once (because they might be subscribed to multiple of the updated signals)
			val subscribersToNotify = subscribers.filter(subscription =>
				updatedSignals.contains(subscription._1)
			).flatMap(e => e._2).toList.distinct
			val futures = subscribersToNotify.map(subscriber => subscriber ? Invoke(signals))

			log.info(s"Informed ${subscribersToNotify.length} subscribers about a change of ${updatedSignals.length} signals ($updatedSignals)")

			scheduledUpdates.clear()

			Future.sequence(futures.toList)
		} flatMap { x =>
			// TODO can we make this non-recursive?
			runDeltaCycle(cycle + 1)
		}
	}
}
