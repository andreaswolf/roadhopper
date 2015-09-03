/*
 * Copyright (c) 2015 Andreas Wolf
 *
 * See te LICENSE file in the project root for further copyright information.
 */

package info.andreaswolf.roadhopper.measurements

import com.emotioncity.soriento.ODocumentReader
import com.emotioncity.soriento.RichODocumentImpl._
import com.emotioncity.soriento.annotations.EmbeddedList
import com.orientechnologies.orient.core.db.record.OTrackedList
import com.orientechnologies.orient.core.record.impl.ODocument
import scala.collection.JavaConverters._
import scala.collection.JavaConversions._


object MeasurementGroup {

	implicit object MeasurementGroupReader extends ODocumentReader[MeasurementGroup] {
		def read(oDocument: ODocument): MeasurementGroup = {
			new MeasurementGroup(
				// this typed get() call is possible because we imported implicit conversions from soriento.RichODocumentImpl
				oDocument.get[String]("name").get,
				oDocument.get[OTrackedList[String]]("measurements").get.toList
			)
		}
	}

}

case class MeasurementGroup(name: String, @EmbeddedList measurements: List[String])
