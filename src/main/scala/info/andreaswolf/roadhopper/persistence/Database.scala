/*
 * Copyright (c) 2015 Andreas Wolf
 *
 * See te LICENSE file in the project root for further copyright information.
 */

package info.andreaswolf.roadhopper.persistence

import com.emotioncity.soriento.{Dsl, ODb}
import com.graphhopper.util.shapes.GHPoint3D
import com.orientechnologies.orient.core.db.document.ODatabaseDocumentTx
import com.orientechnologies.orient.server.OServerMain
import info.andreaswolf.roadhopper.measurements.{MeasurementGroup, Measurement}
import com.emotioncity.soriento.RichODatabaseDocumentImpl._


class Database extends ODb with Dsl {

	val server = OServerMain.create
	val uri: String = "plocal:${ORIENTDB_HOME}/database/roadhopper"
	implicit private val _conn = new ODatabaseDocumentTx(uri)


	startServer()
	connectToDB()
	registerClasses()


	def startServer(): Unit = {
		server.startup()
		server.activate()
	}

	def connectToDB(): Unit = {
		if (!_conn.exists) {
			_conn.create()
		} else {
			_conn.open("admin", "admin")
		}
	}

	def registerClasses(): Unit = {
		createOClass[Measurement]
		_conn.getMetadata.getSchema.reload()

		createOClass[MeasurementGroup]
		_conn.getMetadata.getSchema.reload()
	}

	def conn: ODatabaseDocumentTx = {
		if (!_conn.isActiveOnCurrentThread)
			_conn.activateOnCurrentThread()

		_conn
	}

}
