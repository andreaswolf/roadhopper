/*
 * Copyright (c) 2015 Andreas Wolf
 *
 * See te LICENSE file in the project root for further copyright information.
 */

package info.andreaswolf.roadhopper.server

import javax.servlet.http.{HttpServletRequest, HttpServletResponse}

import com.google.inject.Inject
import info.andreaswolf.roadhopper.simulation.SimulationRepository
import info.andreaswolf.roadhopper.simulation.signals.SignalState
import org.json.{JSONObject, JSONStringer}
import org.slf4j.LoggerFactory

import scala.collection.JavaConversions


/**
 * Fetches signal values for a given simulation and returns them to the client.
 */
class SignalValuesServlet extends BaseServlet {

	val log = LoggerFactory.getLogger(this.getClass)

	@Inject val simulationRepository: SimulationRepository = null

	override def doGet(req: HttpServletRequest, resp: HttpServletResponse): Unit = {
		if (req.getParameter("simulation") == null) {
			writeError(resp, 400, s"No simulation id given.")
			return
		}

		val simulationId = req.getParameter("simulation")
		if (!simulationRepository.has(simulationId)) {
			writeError(resp, 404, s"Simulation with id $simulationId not found.")
			return
		}
		val simulation = simulationRepository.getByIdentifier(simulationId)

		val signals: List[String] = req.getParameterValues("signal") match {
			case x if x == null => List("v", "a")
			case x if x.isEmpty => List("v", "a")
			case x => x.toList
		}

		val serializer = new JsonSignalValueSerializer(signals)
		val output = new JSONStringer
		output.`object`()
		simulation.result.signals.toSeq.sortBy(_._1).foreach {case (time, signalValues) =>
			output.key(time.toString).value(serializer.serializeState(signalValues))
		}
		output.endObject()

		resp.getWriter.append(output.toString)
	}

	class JsonSignalValueSerializer(val signals: List[String]) {
		def serializeState(signalState: SignalState): JSONObject = {
			val interestingValues = signalState.values.filterKeys(name => signals.contains(name))

			new JSONObject(JavaConversions.mapAsJavaMap(interestingValues))
		}
	}

}
