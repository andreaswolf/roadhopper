package info.andreaswolf.roadhopper.server

import java.util
import javax.servlet.http.{HttpServletResponse, HttpServletRequest}

import akka.actor.Props
import com.google.inject.Inject
import com.graphhopper.http.GraphHopperServlet
import com.graphhopper.util.StopWatch
import com.graphhopper.util.shapes.BBox
import info.andreaswolf.roadhopper.RoadHopper
import info.andreaswolf.roadhopper.road.RouteFactory
import info.andreaswolf.roadhopper.simulation._
import org.json.JSONObject

import scala.collection.convert.decorateAll._
import scala.collection.{JavaConversions, mutable}

class SimulationServlet extends GraphHopperServlet {

	@Inject var roadHopper: RoadHopper = null

	override def doGet(req: HttpServletRequest, res: HttpServletResponse): Unit = {
		val points = getPoints(req, "point")

		val routeFactory = new RouteFactory(roadHopper)
		val route = routeFactory.simplify(routeFactory.getRoute(points.asScala.toList).parts, 2.0)

		val simulation: ActorBasedSimulation = new ActorBasedSimulation(route)
		val result = new SimulationResult()

		simulation.registerActor(
			Props(new SimulationResultLogger(result, simulation.timer, 250, simulation.vehicle)),
			"resultWriter"
		)

		val sw = new StopWatch().start()

		// TODO include bounding box here; see JsonWriter for details

		val jsonContents = new mutable.HashMap[String, Object]()
		val encodedRoute: util.List[AnyRef] = new GeoJsonEncoder().encodeRoute(route)
		jsonContents.put("points", encodedRoute)
		jsonContents.put("info", new util.HashMap[String, Object]())

		simulation.actorSystem.registerOnTermination({
			jsonContents.put("simulation", serializeSimulationResult(result))
			val json = new JSONObject(JavaConversions.mapAsJavaMap(jsonContents))
			res.getWriter.append(json.toString)
		})

		simulation.start()
		simulation.actorSystem.awaitTermination()

		res.setStatus(200)
		res.setCharacterEncoding("UTF-8")
	}

	def serializeSimulationResult(result: SimulationResult) = {
		val serialized = new mutable.HashMap[Int, Object]()
		for ((time: Int, state: VehicleState) <- result.map) {
			val stateInfo = new mutable.HashMap[String, Any]()

			stateInfo.put("position", state.position.getOrElse("undefined"))
			stateInfo.put("speed", state.speed.toDouble)
			stateInfo.put("direction", state.orientation.toDouble)
			serialized.put(time, JavaConversions.mutableMapAsJavaMap(stateInfo))
		}
		JavaConversions.mutableMapAsJavaMap(serialized)
	}
}
