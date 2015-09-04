/*
 * Copyright (c) 2015 Andreas Wolf
 *
 * See te LICENSE file in the project root for further copyright information.
 */

package info.andreaswolf.roadhopper.server

import javax.servlet.http.{HttpServletRequest, HttpServletResponse}

import com.google.inject.Inject
import com.graphhopper.GraphHopper
import com.graphhopper.matching.{EdgeMatch, LocationIndexMatch, MapMatching}
import com.graphhopper.storage.NodeAccess
import com.graphhopper.storage.index.LocationIndexTree
import com.graphhopper.util.{CmdArgs, GPXEntry}
import info.andreaswolf.roadhopper.measurements.{DataPoint, Measurement, MeasurementRepository}
import info.andreaswolf.roadhopper.persistence.Database
import org.json.{JSONString, JSONArray, JSONObject, JSONStringer}
import org.slf4j.LoggerFactory

import scala.StringBuilder
import scala.collection.mutable
import scala.collection.mutable.ListBuffer

/**
 * Servlet to convert data from the measurements done by HEV to a format usable for display in the map.
 */
class MeasurementsServlet extends BaseServlet {

	@Inject
	var args: CmdArgs = null

	@Inject var database: Database = null

	@Inject var measurementRepository: MeasurementRepository = null

	@Inject var hopper: GraphHopper = null

	val measurements = new mutable.TreeSet[DataPoint]()

	val log = LoggerFactory.getLogger("MeasurementsServlet")


	def getParameter(req: HttpServletRequest, name: String, default: String = "") = {
		val params: Array[String] = req.getParameterMap.get(name)
		if (params != null && params.length > 0) {
			params.apply(0)
		} else {
			default
		}
	}

	override def doGet(req: HttpServletRequest, resp: HttpServletResponse): Unit = {
		val measurement = req.getParameter("name")

		val json = new JSONStringer
		if (measurement == null) {
			val groups = measurementRepository.findAllGroups
			json.`object`().key("files").`object`()

			groups.sortBy(_.name).foreach({ g =>
				json.key(g.name).array()
				g.measurements.foreach({ m => json.value(m) })
				json.endArray()
			})

			json.endObject().endObject()
		} else {
			val foundMeasurements: List[Measurement] = measurementRepository.findByName(measurement)
			if (foundMeasurements.isEmpty) {
				writeError(resp, 404, s"Measurement $measurement not found")
			}
			val measurementObject = foundMeasurements.head

			resp.getWriter.append(new JsonExporter().exportMeasurement(measurementObject))
		}
	}



	trait MeasurementExporter {
		def exportMeasurement(measurementObject: Measurement): String
	}



	class JsonExporter extends MeasurementExporter {

		class JSONMeasurement(val measurement: Measurement) extends JSONString {
			/**
			 * Converts the data points of a given measurement to a JSON object with the time as the key.
			 */
			def toJSONString: String = {
				val json = new JSONStringer().`object`()

				for (datum <- measurement.points) { // .seq.sortWith((a, b) => a.date < b.date)
					json.key(datum.date.toString).value(datum: JSONObject)
				}
				json.endObject()
				json.toString
			}
		}

		def exportMeasurement(measurementObject: Measurement): String = {
			val json = new JSONStringer

			json.`object`()

			//json.key("duration").value(measurementObject.duration)

			json.key("measurements").value(new JSONMeasurement(measurementObject))

			try {
				implicit val nodes: NodeAccess = hopper.getGraphHopperStorage.getNodeAccess
				val matchedCoordinates: JSONArray = matchCoordinates(measurementObject): JSONArray
				json.key("matchedRoad").value(matchedCoordinates)
			} catch {
				case e: RuntimeException =>
					json.key("matchedRoad").value("Matching error: " + e.getMessage)
			}

			json.endObject().toString
		}
	}

	def convertMeasurementsFile(measurement: Measurement): JSONObject = {
		val json = new JSONObject()

		for (datum <- measurement.points) {
			json.put(datum.date.toString, datum: JSONObject)
		}
		json
	}

	/**
	 * Converts a bunch of edges into a continuous list of coordinates fit for GeoJSON usage.
	 *
	 * @return A list of [lon,lat] pairs
	 */
	implicit def convertCoordinatesToJSON(edges: List[EdgeMatch])(implicit nodes: NodeAccess): JSONArray = {
		val result = new JSONArray()
		if (edges.isEmpty) {
			return result
		}

		val node: Int = edges.head.getEdgeState.getBaseNode
		result.put(new JSONArray().put(nodes.getLon(node)).put(nodes.getLat(node)))
		edges.foreach(edge => {
			// add the
			val wayNodes = edge.getEdgeState.fetchWayGeometry(2)
			for (i <- 0 to wayNodes.size() - 1) {
				result.put(new JSONArray().put(wayNodes.getLon(i)).put(wayNodes.getLat(i)))
			}
		})

		result
	}

	def matchCoordinates(measurement: Measurement): List[EdgeMatch] = {
		val graph = hopper.getGraphHopperStorage
		val locationIndex = new LocationIndexMatch(graph, hopper.getLocationIndex.asInstanceOf[LocationIndexTree])
		val mapMatching = new MapMatching(graph, locationIndex, hopper.getEncodingManager.getEncoder("car"))

		val gpxPointsBuffer = new ListBuffer[GPXEntry]()
		measurement.points.foreach(dp => gpxPointsBuffer.append(new GPXEntry(dp.position, dp.date)))

		import scala.collection.JavaConversions._
		mapMatching.doWork(gpxPointsBuffer.toList).getEdgeMatches.toList
	}

}
