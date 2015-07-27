package info.andreaswolf.roadhopper.simulation

import akka.actor.{ActorSystem, Props}
import akka.pattern.ask
import akka.testkit.{ImplicitSender, TestKit, TestProbe}
import akka.util.Timeout
import com.graphhopper.util.shapes.GHPoint3D
import info.andreaswolf.roadhopper.road.{RoadBuilder, Route}
import junit.framework.TestCase
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpecLike}

import scala.concurrent.Await
import scala.concurrent.duration._
import scala.util.Success

class TwoStepJourneyActorTest(_system: ActorSystem) extends TestKit(_system)
with WordSpecLike with ImplicitSender with Matchers with BeforeAndAfterAll {

	implicit val timeout = Timeout(10 seconds)
	import _system.dispatcher


	def this() = this(ActorSystem("ActorTest"))

	override def afterAll {
		TestKit.shutdownActorSystem(system)
	}

	"JourneyActor" must {
		"must return all segments at beginning of road" in new TestCase {
			val route = new RoadBuilder(new GHPoint3D(49.0, 8.0, 0.0)) addSegment(100, 0.0) addSegment(200, 20.0) buildRoute
			val subject = system.actorOf(Props(new TwoStepJourneyActor(TestProbe().ref, TestProbe().ref, route)))

			val response = subject ? RequestRoadAhead(0)
			Await.ready(response, 1 seconds)

			val Success(RoadAhead(_, roadAhead, _)) = response.value.get
			assert(route.getRoadSegments == roadAhead)
		}

		"must return only the second segment when the first segment has passed" in new TestCase {
			val route = new RoadBuilder(new GHPoint3D(49.0, 8.0, 0.0)) addSegment(100, 0.0) addSegment(200, 20.0) buildRoute
			val subject = system.actorOf(Props(new TwoStepJourneyActor(TestProbe().ref, TestProbe().ref, route)))

			val response = subject ? RequestRoadAhead(100)
			Await.ready(response, 1 second)

			val Success(RoadAhead(_, roadAhead, _)) = response.value.get
			assert(roadAhead.length == 1)
			assert(roadAhead.head == route.getRoadSegments.apply(1))
		}
	}

}
