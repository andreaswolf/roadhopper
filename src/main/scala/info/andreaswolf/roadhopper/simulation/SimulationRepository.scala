/*
 * Copyright (c) 2015 Andreas Wolf
 *
 * See te LICENSE file in the project root for further copyright information.
 */

package info.andreaswolf.roadhopper.simulation

import org.slf4j.LoggerFactory

import scala.collection.mutable


class SimulationRepository {

	type T = ActorBasedSimulation

	val log = LoggerFactory.getLogger(this.getClass)

	private val simulations = new mutable.HashMap[String, T]()


	def add(sim: T) = {
		simulations.put(sim.identifier, sim)
		log.debug(s"Added simulation with id ${sim.identifier}")

		Thread.sleep(5000)
	}

	def getByIdentifier[T](id: String) = simulations.get(id).get

	def has(id: String) = simulations.contains(id)

}
