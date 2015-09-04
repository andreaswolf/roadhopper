/*
 * Copyright (c) 2015 Andreas Wolf
 *
 * See te LICENSE file in the project root for further copyright information.
 */

package info.andreaswolf.roadhopper.measurements

import com.emotioncity.soriento.ODocumentReader
import com.emotioncity.soriento.RichODocumentImpl._
import com.emotioncity.soriento.annotations.EmbeddedList
import com.graphhopper.util.shapes.GHPoint3D
import com.orientechnologies.orient.core.record.impl.ODocument
import info.andreaswolf.roadhopper.road.RoadSegment

import scala.collection.LinearSeq


object Measurement {

	implicit object MeasurementReader extends ODocumentReader[Measurement] {
		def read(oDocument: ODocument): Measurement = {
			new Measurement(
				// this typed get() call is possible because we imported implicit conversions from soriento.RichODocumentImpl
				oDocument.get[String]("name").get,
				oDocument.getAsList[DataPoint]("points").get,
				oDocument.getAsList[RoadSegment]("road")(RoadSegmentReader).getOrElse(List[RoadSegment]())
			)
		}
	}

	implicit object RoadSegmentReader extends ODocumentReader[RoadSegment] {
		override def read(oDocument: ODocument): RoadSegment = {
			new RoadSegment(
				oDocument.get[GHPoint3D]("start").get,
				oDocument.get[GHPoint3D]("end").get,
				oDocument.get[Double]("speedLimit").get
			)
		}
	}

}

case class Measurement(name: String, @EmbeddedList points: LinearSeq[DataPoint], road: List[RoadSegment]) {
	lazy val duration = points.length match {
		case x if x <= 2 => 0
		case x => points.reverse.head.date - points.head.date
	}
}
