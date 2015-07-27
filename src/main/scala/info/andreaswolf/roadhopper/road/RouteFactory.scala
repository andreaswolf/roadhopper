package info.andreaswolf.roadhopper.road

import com.graphhopper.routing.Path
import com.graphhopper.storage.NodeAccess
import com.graphhopper.storage.extensions.RoadSignEncoder
import com.graphhopper.util.shapes.{GHPoint, GHPoint3D}
import info.andreaswolf.roadhopper.RoadHopper
import info.andreaswolf.roadhopper.server.RouteCalculator
import org.slf4j.LoggerFactory

import util.control.Breaks._
import scala.collection.convert.decorateAll._
import scala.collection.mutable.ListBuffer

class RouteFactory(val hopper: RoadHopper) {

	val log = LoggerFactory.getLogger("RouteFactory")

	val calculator = new RouteCalculator(hopper)
	calculator.setLocale("de")

	def getRoute(points: List[GHPoint]): Route = {
		val paths = calculator.getPaths("car", points.asJava)

		createRouteFromPaths(paths.asScala.toList)
	}

	def createRouteFromPaths(paths: List[Path]): Route = {
		import scala.collection.JavaConversions._
		val segments = new ListBuffer[RoadSegment]
		var lastPoint: Option[GHPoint3D] = None

		val signEncoder: RoadSignEncoder = new RoadSignEncoder(hopper.getGraph)
		// TODO make the encoder name configurable
		val flagEncoder = hopper.getGraph.getEncodingManager.getEncoder("car")
		val nodeAccess: NodeAccess = hopper.getGraph.getNodeAccess

		for (i <- paths.indices) {
			// NOTE the first and last edges might be incomplete, as we enter the road through it! -> conclusion: do not use
			// the edges for any calculations, but instead rely on the points
			for (edge <- paths.get(i).calcEdges()) {
				val flags = hopper.getQueryGraph.getEdgeProps(edge.getEdge, edge.getAdjNode).getFlags
				val maximumSpeed = flagEncoder.getSpeed(flags) / 3.6 // speed is stored in km/h, but we need m/s
				log.debug(f"Found edge properties for edge ${edge.getEdge} with max speed $maximumSpeed%.2f m/s")

				// TODO move creating the road segments for one edge to a separate method
				for (point <- edge.fetchWayGeometry(3)) {
					breakable {
						if (lastPoint.isDefined && point.equals(lastPoint.get)) {
							break
						}

						lastPoint.foreach(p => {
							segments append new RoadSegmentBuilder()
								.start(p.getLat, p.getLon, p.getEle)
								.end(point.getLat, point.getLon, point.getEle)
								.speedLimit(maximumSpeed)
								.build
						})
						lastPoint = Some(point)
					}
				}
				val endNodeId: Int = edge.getAdjNode
				if (signEncoder.hasTrafficLight(endNodeId)) {
					segments.last.setRoadSign(new TrafficLight(endNodeId,
						new GHPoint3D(nodeAccess.getLat(endNodeId), nodeAccess.getLon(endNodeId), 0.0)
					))
				} else if (signEncoder.hasStopSign(endNodeId)) {
					segments.last.setRoadSign(new StopSign(endNodeId,
						new GHPoint3D(nodeAccess.getLat(endNodeId), nodeAccess.getLon(endNodeId), 0.0)
					))
				}
			}
		}

		new Route(segments.result())
	}

	def simplify(parts: List[RoadSegment], delta: Double = 2.0): Route = {
		val segments = new ListBuffer[RoadSegment]

		if (parts.isEmpty) {
			return new Route(segments.result())
		}

		// as read access to the head of a list is more convenient, we always insert elements at the beginning -> the result
		// will be inverted
		segments prepend parts.head
		for (currentSegment <- parts.tail) {
			// TODO this code part is really ugly – check if we can improve this
			val lastSegment: RoadSegment = segments.head
			// make sure we have a small change in orientation and the last segment has no road sign at the end
			if (lastSegment.roadSign.isEmpty && Math.abs(
					(lastSegment.orientation - currentSegment.orientation).toDegrees
				) < delta && lastSegment.speedLimit == currentSegment.speedLimit) {

				val newSegment: RoadSegment = new RoadSegment(
					lastSegment.start, currentSegment.end, currentSegment.speedLimit
				)
				newSegment.setRoadSign(currentSegment.roadSign)
				segments update(0, newSegment)
			} else {
				segments prepend currentSegment
			}
		}

		new Route(segments.result().reverse)
	}

}
