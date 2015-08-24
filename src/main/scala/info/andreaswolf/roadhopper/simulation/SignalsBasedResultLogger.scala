package info.andreaswolf.roadhopper.simulation

import akka.actor.{ActorRef, ActorLogging}
import akka.pattern.ask
import com.graphhopper.util.shapes.{GHPoint, GHPoint3D}
import info.andreaswolf.roadhopper.simulation.signals.{SignalState, Process}

import scala.concurrent.{Future, ExecutionContext}
import scala.util.Success

/**
 * Watches the vehicle status and adds them to the passed result object.
 */
class SignalsBasedResultLogger(val result: SimulationResult, val signalBus: ActorRef, val interval: Int = 250)
	extends Process(signalBus) with ActorLogging {

	import context.dispatcher

	val defaultPosition = new GHPoint3D(0.0, 0.0, 0.0)


	/**
	 * The central routine of a process. This is invoked whenever a subscribed signal’s value changes.
	 */
	override def invoke(signals: SignalState): Future[Any] = Future {
		if (time % interval == 0) {
			val state = new VehicleState(
				signals.signalValue[Double]("a", 0.0),
				signals.signalValue[Double]("v", 0.0),
				signals.signalValue[Double]("heading", 0.0), // TODO implement heading!
				Some(signals.signalValue[GHPoint3D]("pos", defaultPosition))
			)
			result.setStatus(time, state)
		}
	}

}
