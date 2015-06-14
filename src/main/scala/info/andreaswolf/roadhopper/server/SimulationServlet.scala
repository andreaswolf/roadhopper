package info.andreaswolf.roadhopper.server

import javax.servlet.http.{HttpServletResponse, HttpServletRequest}

import com.google.inject.Inject
import com.graphhopper.http.GraphHopperServlet
import info.andreaswolf.roadhopper.RoadHopper
import info.andreaswolf.roadhopper.road.RouteFactory
import info.andreaswolf.roadhopper.simulation.{ActorBasedSimulation, Start}

import scala.collection.convert.decorateAll._

class SimulationServlet extends GraphHopperServlet {

	@Inject var roadHopper: RoadHopper = null

	override def doGet(req: HttpServletRequest, res: HttpServletResponse): Unit = {
		val points = getPoints(req, "point")

		val routeFactory = new RouteFactory(roadHopper)
		val route = routeFactory.simplify(routeFactory.getRoute(points.asScala.toList).parts, 2.0)

		val simulation: ActorBasedSimulation = new ActorBasedSimulation(route)

		simulation.start()

		res.setStatus(200)
		res.setCharacterEncoding("UTF-8")
		res.getWriter.println(route.length.floor)
	}
}
