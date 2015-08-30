/*
 * Copyright (c) 2015 Andreas Wolf
 *
 * See te LICENSE file in the project root for further copyright information.
 */

package info.andreaswolf.roadhopper.map

import com.emotioncity.soriento.ODocumentReader
import com.graphhopper.util.shapes.GHPoint3D
import com.orientechnologies.orient.core.record.impl.ODocument
import com.emotioncity.soriento.RichODocumentImpl._
import org.json.JSONObject


object Point {
	implicit def pointToGHPoint3D(point: Point): GHPoint3D = {
		new GHPoint3D(point.lat, point.lon, point.elevation)
	}

	implicit object PointReader extends ODocumentReader[Point] {
		def read(oDocument: ODocument): Point = {
			new Point(
				// this typed get() call is possible because we imported implicit conversions from soriento.RichODocumentImpl
				oDocument.get[Double]("lat").get,
				oDocument.get[Double]("lon").get,
				oDocument.get[Double]("elevation").get
			)
		}
	}

	implicit def convert(point: Point): JSONObject = {
		new JSONObject().put("lat", point.lat).put("lon", point.lon).put("elevation", point.ele)
	}
}

/**
 * Simple representation of a coordinate on the earth’s surface.
 */
case class Point(lat: Double, lon: Double, elevation: Double = Double.NaN)
