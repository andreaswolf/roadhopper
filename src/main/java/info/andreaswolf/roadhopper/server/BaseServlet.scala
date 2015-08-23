/*
 * Copyright (c) 2015 Andreas Wolf
 *
 * See te LICENSE file in the project root for further copyright information.
 */

package info.andreaswolf.roadhopper.server

import javax.servlet.http.{HttpServletResponse, HttpServlet}

import info.andreaswolf.roadhopper.simulation.{VehicleState, SimulationResult}
import org.json.JSONStringer

import scala.collection.{JavaConversions, mutable}


class BaseServlet extends HttpServlet {

	def writeError(resp: HttpServletResponse, code: Int, message: String): Unit = {
		resp.getWriter.append(new JSONStringer().`object`()
			.key("code").value(code)
			.key("error").value(message)
			.endObject().toString
		)
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
