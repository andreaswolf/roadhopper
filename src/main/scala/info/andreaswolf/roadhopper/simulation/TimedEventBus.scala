package info.andreaswolf.roadhopper.simulation

import akka.actor.ActorRef
import akka.event.{LookupClassification, EventBus}

// see http://doc.akka.io/docs/akka/snapshot/scala/event-bus.html

class TimedEventBus extends EventBus with LookupClassification {
	type Event = MsgEnvelope
	type Classifier = String
	type Subscriber = ActorRef

	override protected def mapSize(): Int = 6

	override protected def publish(event: Event, subscriber: Subscriber): Unit = subscriber ! event.payload

	override protected def classify(event: Event): Classifier = event.topic

	override protected def compareSubscribers(a: Subscriber, b: Subscriber): Int = a.compareTo(b)
}

final case class MsgEnvelope(topic: String, payload: Any)

