/*
 * Copyright (c) 2015 Andreas Wolf
 *
 * See te LICENSE file in the project root for further copyright information.
 */

package info.andreaswolf.roadhopper.server

import java.io.File
import javax.servlet.http.{HttpServlet, HttpServletRequest, HttpServletResponse}

import com.google.inject.Inject
import com.graphhopper.util.CmdArgs
import info.andreaswolf.roadhopper.measurements.{Measurement, DataPoint, MeasurementFile, MeasurementRepository}
import info.andreaswolf.roadhopper.persistence.Database
import org.json.{JSONArray, JSONObject, JSONStringer}
import org.slf4j.LoggerFactory

import scala.collection.{JavaConversions, mutable}
import scala.io.Source

/**
 * Servlet to convert data from the measurements done by HEV to a format usable for display in the map.
 */
class MeasurementsServlet extends HttpServlet {

	@Inject
	var args: CmdArgs = null

	@Inject var database: Database = null

	@Inject var measurementRepository: MeasurementRepository = null

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
		val group = req.getParameter("name")

		val json = new JSONStringer
		if (group == null) {
			val groups = measurementRepository.findAllGroups
			json.`object`().key("files").`object`()

			groups.sortBy(_.name).foreach({ g =>
				json.key(g.name).array()
				g.measurements.foreach({ m => json.value(m)})
				json.endArray()
			})

			json.endObject().endObject()
		} else {
			val groupObject = measurementRepository.findByName(group).head

			json.`object`()

			json.key("measurements").value(convertMeasurementsFile(groupObject))

			json.endObject()
		}
		resp.getWriter.append(json.toString)
	}

	def convertMeasurementsFile(measurement: Measurement): JSONObject = {
		val json = new JSONObject()

		for (datum <- measurement.points) {
			json.put(datum.date.toString, datum: JSONObject)
		}
		json
	}


}
