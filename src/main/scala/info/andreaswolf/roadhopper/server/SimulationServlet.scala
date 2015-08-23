package info.andreaswolf.roadhopper.server

import java.util
import javax.servlet.http.{HttpServletResponse, HttpServletRequest}

import akka.actor.Props
import com.google.inject.Inject
import com.graphhopper.http.GraphHopperServlet
import com.graphhopper.util.StopWatch
import com.graphhopper.util.shapes.BBox
import info.andreaswolf.roadhopper.RoadHopper
import info.andreaswolf.roadhopper.road.{Route, RouteRepository, RouteFactory}
import info.andreaswolf.roadhopper.simulation._
import org.json.JSONObject

import scala.collection.convert.decorateAll._
import scala.collection.{JavaConversions, mutable}


/**
 * HTTP endpoint to start a simulation.
 */
class SimulationServlet extends GraphHopperServlet {

	@Inject var roadHopper: RoadHopper = null

	@Inject val routeRepository: RouteRepository = null

	@Inject val simulationRepository: SimulationRepository = null

	override def doGet(req: HttpServletRequest, res: HttpServletResponse): Unit = {
		var route: Route = null
		if (req.getParameter("route") != null) {
			val routeId: String = req.getParameter("route")
			if (!routeRepository.has(routeId)) {
				writeError(res, 404, s"Route with id $routeId not found.")
				return
			}

			route = routeRepository.getByIdentifier(routeId).get
		} else {
			val points = getPoints(req, "point")
			if (points.size() < 2) {
				writeError(res, 400, "At least two points must be given for simulation")
			}
			val routeFactory = new RouteFactory(roadHopper)
			route = routeFactory.simplify(routeFactory.getRoute(points.asScala.toList).parts, 2.0)
		}

		val result = new SimulationResult()
		val simulation: ActorBasedSimulation = new ActorBasedSimulation(route, result)
		simulationRepository.add(simulation)

		simulation.registerActor(
			Props(new SimulationResultLogger(result, simulation.timer, 250, simulation.vehicle)),
			"resultWriter"
		)

		val sw = new StopWatch().start()

		simulation.start()

		// TODO include bounding box here; see JsonWriter for details

		val jsonContents = new mutable.HashMap[String, Object]()
		val encodedRoute: util.List[AnyRef] = new GeoJsonEncoder().encodeRoute(route)
		jsonContents.put("points", encodedRoute)
		jsonContents.put("simulation", simulation.identifier)
		jsonContents.put("route", simulation.route.identifier)
		jsonContents.put("info", new util.HashMap[String, Object]())
		jsonContents.put("status", if (simulation.isFinished) "finished" else "running")

		val json = new JSONObject(JavaConversions.mapAsJavaMap(jsonContents))
		res.getWriter.append(json.toString)

		res.setStatus(200)
		res.setCharacterEncoding("UTF-8")
	}

}
