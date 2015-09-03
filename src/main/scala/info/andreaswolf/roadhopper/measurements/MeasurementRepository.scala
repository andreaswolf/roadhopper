/*
 * Copyright (c) 2015 Andreas Wolf
 *
 * See te LICENSE file in the project root for further copyright information.
 */

package info.andreaswolf.roadhopper.measurements

import com.emotioncity.soriento.Dsl
import com.emotioncity.soriento.RichODatabaseDocumentImpl._
import com.google.inject.Inject
import com.orientechnologies.orient.core.record.impl.ODocument
import info.andreaswolf.roadhopper.persistence.Database

// RichODatabaseDocumentImpl._ is required for queryBySql() to work


class MeasurementRepository extends Dsl {

	@Inject var database: Database = null


	def findAllGroups: List[MeasurementGroup] = database.conn.queryBySql[MeasurementGroup]("select * from MeasurementGroup")

	def findAll: List[Measurement] = database.conn.queryBySql[Measurement]("select * from Measurement")

	def findGroupByName(name: String): List[MeasurementGroup] =
		database.conn.queryBySql[MeasurementGroup]("SELECT * FROM MeasurementGroup WHERE name = \"%s\"".format(name)) match {
			case null => List()
			case Nil => List()
			case x => x
		}

	def findByName(name: String): List[Measurement] =
		database.conn.queryBySql[Measurement]("SELECT * FROM Measurement WHERE name = \"%s\"".format(name)) match {
			case null => List()
			case Nil => List()
			case x => x
		}

	def add(measurement: Measurement): ODocument = database.conn.save(measurement)

	def add(group: MeasurementGroup): ODocument = database.conn.save(group)

}
