package info.andreaswolf.roadhopper.road

import com.graphhopper.routing.Path
import com.graphhopper.storage.NodeAccess
import com.graphhopper.storage.extensions.RoadSignEncoder
import com.graphhopper.util.shapes.{GHPoint, GHPoint3D}
import info.andreaswolf.roadhopper.RoadHopper
import info.andreaswolf.roadhopper.server.RouteCalculator

import util.control.Breaks._
import scala.collection.convert.decorateAll._
import scala.collection.mutable.ListBuffer

class RouteFactory(val hopper: RoadHopper) {

	val calculator = new RouteCalculator(hopper)
	calculator.setLocale("de")

	def getRoute(points: List[GHPoint]): Route = {
		val paths = calculator.getPaths("car", points.asJava)

		createRouteFromPaths(paths.asScala.toList)
	}

	def createRouteFromPaths(paths: List[Path]): Route = {
		import scala.collection.JavaConversions._
		val segments: ListBuffer[RoutePart] = new ListBuffer[RoutePart]
		var lastPoint: Option[GHPoint3D] = None

		val signEncoder: RoadSignEncoder = new RoadSignEncoder(hopper.getGraph)
		val nodeAccess: NodeAccess = hopper.getGraph.getNodeAccess

		for (i <- paths.indices) {
			// NOTE the first and last edges might be incomplete, as we enter the road through it! -> conclusion: do not use
			// the edges for any calculations, but instead rely on the points
			for (edge <- paths.get(i).calcEdges()) {
				if (signEncoder.hasTrafficLight(edge.getBaseNode)) {
					segments append new TrafficLight(edge.getBaseNode,
						new GHPoint3D(nodeAccess.getLat(edge.getBaseNode), nodeAccess.getLon(edge.getBaseNode), 0.0)
					)
				}
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

	def simplify(parts: List[RoutePart], delta: Double = 2.0): Route = {
		val segments: ListBuffer[RoutePart] = new ListBuffer[RoutePart]

		if (parts.isEmpty) {
			return new Route(segments.result())
		}

		// as read access to the head of a list is more convenient, we always insert elements at the beginning -> the result
		// will be inverted
		segments prepend parts.head
		for (part <- parts.tail) {
			if (!segments.head.isInstanceOf[RoadSegment] || !part.isInstanceOf[RoadSegment]) {
				segments prepend part
			} else {
				// TODO this code part is really ugly – check if we can improve this
				val lastSegment: RoadSegment = segments.head.asInstanceOf[RoadSegment]
				if (Math.abs(
						(lastSegment.orientation - part.asInstanceOf[RoadSegment].orientation).toDegrees
					) < delta) {

					val totalLength = lastSegment.length + part.asInstanceOf[RoadSegment].length
					val mediumOrientation = (lastSegment.orientation + part.asInstanceOf[RoadSegment].orientation) / 2

					val newSegment: RoadSegment = RoadSegment.fromPoints(lastSegment.start, part.asInstanceOf[RoadSegment].end)
					segments update(0, newSegment)
				} else {
					segments prepend part
				}
			}
		}

		new Route(segments.result().reverse)
	}

}
