/*
 * Copyright (c) 2015 Andreas Wolf
 *
 * See te LICENSE file in the project root for further copyright information.
 */

package info.andreaswolf.roadhopper.measurements

import com.emotioncity.soriento.ODocumentReader
import com.emotioncity.soriento.RichODocumentImpl._
import com.emotioncity.soriento.annotations.Embedded
import com.orientechnologies.orient.core.record.impl.ODocument
import info.andreaswolf.roadhopper.map.Point
import org.json.JSONObject


object DataPoint {

	implicit object DataPointReader extends ODocumentReader[DataPoint] {
		def read(oDocument: ODocument): DataPoint = {
			new DataPoint(
				// this typed get() call is possible because we imported implicit conversions from soriento.RichODocumentImpl
				oDocument.get[Long]("date").get,
				oDocument.getAs[Point]("position").get,
				oDocument.get[Double]("velocity").get,
				oDocument.get[Double]("orientation").get
			)
		}
	}

	implicit def convert(point: DataPoint): JSONObject = {
		new JSONObject()
			.put("date", point.date)
			.put("position", point.position: JSONObject)
			.put("speed", point.velocity)
			.put("orientation", point.orientation)
	}

}

case class DataPoint(date: Long, @Embedded position: Point, velocity: Double, orientation: Double)
	extends Ordered[DataPoint] {

	def compare(that: DataPoint): Int = this.date compare that.date
}
