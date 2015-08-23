/*
 * Copyright (c) 2015 Andreas Wolf
 *
 * See te LICENSE file in the project root for further copyright information.
 */

package info.andreaswolf.roadhopper.server

import javax.servlet.http.{HttpServletRequest, HttpServletResponse}

import com.google.inject.Inject
import info.andreaswolf.roadhopper.simulation.{VehicleState, SimulationResult, SimulationRepository}
import org.json.{JSONWriter, JSONStringer}

import scala.collection.{JavaConversions, mutable}


class SimulationStatusServlet extends BaseServlet {

	@Inject val simulationRepository: SimulationRepository = null

	override def doGet(req: HttpServletRequest, resp: HttpServletResponse): Unit = {
		val simulationId = req.getParameter("id")

		if (simulationId == null) {
			writeError(resp, 404, s"No simulation id given")
			return
		}
		if (!simulationRepository.has(simulationId)) {
			writeError(resp, 404, s"Simulation with id $simulationId not found")
			return
		}

		val simulation = simulationRepository.getByIdentifier(simulationId)

		val status: JSONWriter = new JSONStringer().`object`()
			.key("simulation").value(simulationId)
			.key("status").value(if (simulation.isFinished) "finished" else "running")

		if (simulation.isFinished) {
			status.key("result").value(serializeSimulationResult(simulation.result))
		}

		resp.getWriter.append(status.endObject().toString)
	}
}
