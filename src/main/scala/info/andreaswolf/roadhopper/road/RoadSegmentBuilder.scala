/*
 * Copyright (c) 2015 Andreas Wolf
 *
 * See te LICENSE file in the project root for further copyright information.
 */

package info.andreaswolf.roadhopper.road

import com.graphhopper.reader.dem.{ElevationProvider, HighPrecisionSRTMProvider}
import com.graphhopper.util.shapes.GHPoint3D

/**
 * Flexible builder class for road segments.
 */
class RoadSegmentBuilder {

	/**
	 * The earth’s radius as defined for WGS84.
	 */
	val R = 6371000

	var eleProvider: ElevationProvider = new HighPrecisionSRTMProvider



	private var _start: Option[GHPoint3D] = None
	private var _end: Option[GHPoint3D] = None

	private var _speedLimit: Option[Double] = None

	private var _name: Option[String] = None

	def start = _start

	def start_=(point: GHPoint3D) = _start = Some(point)

	def start(point: GHPoint3D): RoadSegmentBuilder = {
		this.start = point
		this
	}

	def start(lat: Double, lon: Double, ele: Double): RoadSegmentBuilder = {
		this.start = new GHPoint3D(lat, lon, ele)
		this
	}


	def end = _start

	def end_=(point: GHPoint3D) = _end = Some(point)

	def end(point: GHPoint3D): RoadSegmentBuilder = {
		this.end = point
		this
	}

	def end(lat: Double, lon: Double, ele: Double): RoadSegmentBuilder = {
		this.end = new GHPoint3D(lat, lon, ele)
		this
	}

	def end(length: Int, orientation: Double): RoadSegmentBuilder = {
		// see http://www.movable-type.co.uk/scripts/latlong.html
		val startLat = start.get.lat.toRadians
		val startLon = start.get.lon.toRadians

		val newLat = Math.asin(Math.sin(startLat) * Math.cos(length.toDouble / R) +
			Math.cos(startLat) * Math.sin(length.toDouble / R) * Math.cos(orientation))
		val newLon = startLon + Math.atan2(Math.sin(orientation) * Math.sin(length.toDouble / R) * Math.cos(startLat),
			Math.cos(length.toDouble / R) - Math.sin(startLat) * Math.sin(newLat))

		this.end = new GHPoint3D(newLat.toDegrees, newLon.toDegrees, eleProvider.getEle(newLat.toDegrees, newLon.toDegrees))

		this
	}


	def speedLimit = _speedLimit

	def speedLimit_=(speed: Double) = _speedLimit = Some(speed)

	def speedLimit(speed: Double): RoadSegmentBuilder = {
		this.speedLimit = speed
		this
	}


	def name = _name

	def name_=(name: String) = _name = Some(name)

	def name(name: String): RoadSegmentBuilder = {
		this.name = name
		this
	}


	def build: RoadSegment = {
		// TODO find a better way to only pass a value if it is defined
		val segment = if (_speedLimit.isDefined) {
				new RoadSegment(_start.get, _end.get, speedLimit = _speedLimit.get)
			} else {
				RoadSegment.fromPoints(_start.get, _end.get)
			}
		segment.setRoadName(_name)

		segment
	}
}
