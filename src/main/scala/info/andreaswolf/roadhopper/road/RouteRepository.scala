/*
 * Copyright (c) 2015 Andreas Wolf
 *
 * See te LICENSE file in the project root for further copyright information.
 */

package info.andreaswolf.roadhopper.road

import org.slf4j.LoggerFactory

import scala.collection.mutable


/**
 * In-memory storage for routes. This is used to keep the routing results between calls to the different end points.
 *
 * Each route has an automatically assigned ID that can be used to
 */
class RouteRepository {

	val log = LoggerFactory.getLogger(this.getClass)

	private val routes = new mutable.HashMap[String, Route]()


	def add(route: Route) = {
		routes.put(route.identifier, route)
		log.debug(s"Added route with id ${route.identifier}")
	}

	def getByIdentifier(id: String) = routes.get(id)

	def has(id: String) = routes.contains(id)

}
