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

	/** Defines a signal with the given name in the signal bus. */
	case class DefineSignal(signalName: String)

	/** Schedules an update of a signal value for a time step in the future.
	 *
	 * @param delta The time (from now on) when the signal should be updated
	 */
	case class ScheduleSignalUpdate(delta: Int, signalName: String, newValue: Any)

	/** Updates a signal value. The update is not executed immediately, but held back until the next delta cycle. */
	case class UpdateSignalValue(signalName: String, newValue: Any)

	/** Subscribes a component to a signal by the signal’s name, letting the component be invoked whenever the signal
	  * value changes. */
	case class SubscribeToSignal(signalName: String, subscriber: ActorRef)

}


/**
 * An implementation of the concept of "signals" as the output of components.
 *
 * Signals are declared by a component, other components can subscribe to them (= get notified when the signal changes).
 * With every new time step, an initial update is triggered with the components that listen to the time.
 *
 * The signal bus is invoked by the timer during each time step, and subsequently schedules one or more delta cycles,
 * depending on the behaviour of other components: If a signal is updated during a delta cycle, another delta cycle
 * is scheduled if there are any components listening to that signal. If no further delta cycle is scheduled, the time
 * step is finished and the future sent back to the timer is fulfilled, therefore letting it continue to the next time
 * step.
 *
 * Theoretically, we could create endless loops if there is a chain of signals where the first one depends on a change
 * of the last one. However, this is no real concern when modelling physical systems, as there are always delays due to
 * inertia etc., which let updates happen "far" in the future (as in "in a future timestep") and not "now" (as in "in
 * the next delta cycle").
 *
 * Signal values are not strongly typed, so you can basically write anything you like to them. However, when reading
 * back the value, you might need to apply typecasting, for which you will need to know the type.
 *
 * The time is the only signal pre-defined by the bus, and this is taken directly from the timer at the beginning of
 * each invocation. More signals can be defined by sending
 * [[info.andreaswolf.roadhopper.simulation.signals.SignalBus.DefineSignal]] messages; components can subscribe to
 * these signals with [[info.andreaswolf.roadhopper.simulation.signals.SignalBus.SubscribeToSignal]] messages.
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

	/** All components that are subscribed to any signal. The map is indexed by the signal name and contains a list of
	 *  all subscribers as the value. */
	val subscribers: mutable.HashMap[String, ListBuffer[ActorRef]] = new mutable.HashMap[String, ListBuffer[ActorRef]]()

	/** The updates that are scheduled for the next delta cycle. If this list is not empty, there will definitely be
	 *  another delta cycle. */
	val scheduledUpdates: mutable.HashMap[String, Any] = new mutable.HashMap[String, Any]()

	/** Updates scheduled for a future time step. These will be executed in the first delta cycle of the time step. */
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
			// TODO check if there already is an update scheduled => how to react then? currently the last update wins
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
				// TODO this sometimes breaks; find out why
				// there might not be any scheduled updates now, as a) we are at the beginning of a time step and
				// b) if there were any scheduled updates left in the previous time step, another delta cycle should have been
				// scheduled there; so if there are any updates left, this most likely means we have a race condition somewhere
				assert(scheduledUpdates.isEmpty, "ERROR: Regular updates scheduled for first delta cycle!")

				// get the signal updates that were scheduled for the current time step in earlier time steps (e.g. in
				// dead time parts)
				scheduledUpdates ++= futureScheduledUpdates.remove(time).getOrElse(new mutable.HashMap[String, AnyVal]())

				scheduledUpdates.put("time", time)
			}
			signals = new SignalState(scheduledUpdates, signals)

			val updatedSignals = scheduledUpdates.keys.toList
			// get a list of all subscribers we must notify (by the signals that were updated) and then make sure that
			// each is only called once (because they might be subscribed to multiple of the updated signals)
			val subscriptionsForUpdatedSignals = subscribers.filter(subscription =>
				updatedSignals.contains(subscription._1)
			)
			val subscribersToNotify = subscriptionsForUpdatedSignals.flatMap(e => e._2).toList.distinct

			// notify all subscribers to any of the updated signals
			// TODO this should probably include a list of all signals that were updated
			val notificationFutures = subscribersToNotify.map(subscriber => subscriber ? Invoke(signals))

			log.info(s"Informed ${subscribersToNotify.length} subscribers about a change of ${updatedSignals.length} signals ($updatedSignals)")

			scheduledUpdates.clear()

			Future.sequence(notificationFutures.toList)
		} flatMap { x =>
			// TODO can we make this non-recursive?
			runDeltaCycle(cycle + 1)
		}
	}
}
