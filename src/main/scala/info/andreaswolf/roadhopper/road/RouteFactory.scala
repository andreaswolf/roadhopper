package info.andreaswolf.roadhopper.road

import com.graphhopper.util.shapes.{GHPoint, GHPoint3D}
import info.andreaswolf.roadhopper.RoadHopper
import info.andreaswolf.roadhopper.server.RouteCalculator

import util.control.Breaks._
import scala.collection.JavaConverters._
import scala.collection.mutable.ListBuffer

class RouteFactory(val hopper: RoadHopper) {

	val calculator = new RouteCalculator(hopper)
	calculator.setLocale("de")

	def getRoute(points: List[GHPoint]): Route = {
		import scala.collection.JavaConversions._
		val paths = calculator.getPaths("car", points.asJava)

		val segments : ListBuffer[RoutePart] = new ListBuffer[RoutePart]
		var lastPoint : Option[GHPoint3D] = None

		for (i <- 0 until paths.size()) {
			println(paths.get(i))
			// NOTE the first and last edges might be incomplete, as we enter the road through it! -> conclusion: do not use
			// the edges for any calculations, but instead rely on the points
			for (edge <- paths.get(i).calcEdges()) {
				// TODO move creating the road segments for one edge to a separate method
				for (point <- edge.fetchWayGeometry(3)) {
					breakable {
						if (lastPoint.isDefined && point.equals(lastPoint.get)) {
							break
						}

						lastPoint.foreach(p => {
							segments append RoadSegment.fromCoordinates(p.getLat, p.getLon, point.getLat, point.getLon)
						})
						lastPoint = Some(point)
					}
				}
			}
		}

		new Route(segments.result())
	}
}
