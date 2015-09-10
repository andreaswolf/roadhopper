package info.andreaswolf.roadhopper.road

import com.graphhopper.reader.dem.{HighPrecisionSRTMProvider, ElevationProvider}
import com.graphhopper.util.shapes.GHPoint3D

object RoadSegment {

	/**
	 * The earth’s radius as defined for WGS84.
	 */
	val R = 6371000

	var eleProvider: ElevationProvider = new HighPrecisionSRTMProvider

	/**
	 *
	 * Note that this uses an approximation for the length that treats the line segment as being orthogonal to the
	 * earth’s radius (Pythagorean approximation). This is generally considered safe for the short distances we have.
	 * <http://www.movable-type.co.uk/scripts/latlong.html>
	 *
	 * @return A RoadSegment instance created from the coordinates
	 */
	def fromCoordinates(lat1: Double, lon1: Double, lat2: Double, lon2: Double): RoadSegment = {
		new RoadSegment(
			new GHPoint3D(lat1, lon1, eleProvider.getEle(lat1, lon1)),
			new GHPoint3D(lat2, lon2, eleProvider.getEle(lat2, lon2))
		)
	}

	def fromPoints(start: GHPoint3D, end: GHPoint3D): RoadSegment = {
		new RoadSegment(start, end)
	}

	/**
	 * Creates a road segment from a start point with the given length, pointing in the given direction.
	 *
	 * @param orientation The orientation in radians
	 * @return
	 */
	def fromPoint(start: GHPoint3D, length: Double, orientation: Double): RoadSegment = {
		// see http://www.movable-type.co.uk/scripts/latlong.html
		val startLat = start.lat.toRadians
		val startLon = start.lon.toRadians

		val newLat = Math.asin(Math.sin(startLat) * Math.cos(length / R) +
			Math.cos(startLat) * Math.sin(length / R) * Math.cos(orientation))
		val newLon = startLon + Math.atan2(Math.sin(orientation) * Math.sin(length / R) * Math.cos(startLat),
			Math.cos(length / R) - Math.sin(startLat) * Math.sin(newLat))

		val end = new GHPoint3D(newLat.toDegrees, newLon.toDegrees, eleProvider.getEle(newLat.toDegrees, newLon.toDegrees))

		new RoadSegment(start, end)
	}

	/**
	 * Constructs a road segment from another segment with the given offset from the start. The end point will be the same
	 * as for the base segment
	 *
	 * @param offset The offset from the start in meters
	 * @param base The road segment to use as base
	 * @return
	 */
	def fromExisting(offset: Double, base: RoadSegment) = {
		if (offset > base.length) {
			throw new IllegalArgumentException(f"Offset must be within the base segment’s length: $offset vs. ${base.length}")
		}

		// see http://www.movable-type.co.uk/scripts/latlong.html
		val oldLat = base.start.lat.toRadians
		val oldLon = base.start.lon.toRadians

		val newLat = Math.asin(Math.sin(oldLat) * Math.cos(offset / R) +
			Math.cos(oldLat) * Math.sin(offset / R) * Math.cos(base.orientation))
		val newLon = oldLon + Math.atan2(Math.sin(base.orientation) * Math.sin(offset / R) * Math.cos(oldLat),
			Math.cos(offset / R) - Math.sin(oldLat) * Math.sin(newLat))

		new RoadSegment(
			new GHPoint3D(newLat.toDegrees, newLon.toDegrees, eleProvider.getEle(newLat.toDegrees, newLon.toDegrees)),
			base
		)
	}

	/**
	 * Constructs a road segment from another segment with the given offset from the end. The start point will be the same
	 * as for the base segment
	 *
	 * @param base The road segment to use as base
	 * @param offset The offset from the end in meters
	 * @return
	 */
	def fromExisting(base: RoadSegment, offset: Double) = {
		if (offset > base.length) {
			throw new IllegalArgumentException(f"Offset must be within the base segment’s length: $offset vs. ${base.length}")
		}

		// see http://www.movable-type.co.uk/scripts/latlong.html
		val oldLat = base.end.lat.toRadians
		val oldLon = base.end.lon.toRadians

		val newLat = Math.asin(Math.sin(oldLat) * Math.cos(offset / R) -
			Math.cos(oldLat) * Math.sin(offset / R) * Math.cos(base.orientation))
		val newLon = oldLon - Math.atan2(Math.sin(base.orientation) * Math.sin(offset / R) * Math.cos(oldLat),
			Math.cos(offset / R) - Math.sin(oldLat) * Math.sin(newLat))

		new RoadSegment(base,
			new GHPoint3D(newLat.toDegrees, newLon.toDegrees, eleProvider.getEle(newLat.toDegrees, newLon.toDegrees))
		)
	}

	/**
	 * Returns the length and orientation of the road segment. The returned length is slightly inaccurate, as
	 * the calculation does not take into account the bended earth surface
	 *
	 * @param start The start coordinate
	 * @param end The end coordinate
	 * @return (Double,Double) The length in meters and the (initial) orientation in radians
	 */
	protected def getLengthAndOrientation(start: GHPoint3D, end: GHPoint3D): (Double, Double) = {
		val phi1 = start.lat.toRadians
		val phi2 = end.lat.toRadians
		val deltaLambda = (end.lon - start.lon).toRadians
		val x = deltaLambda * Math.cos((phi1 + phi2) / 2)
		val y = phi2 - phi1
		val length = Math.sqrt(x * x + y * y) * R
		val orientation = Math.atan2(Math.sin(deltaLambda) * Math.cos(phi2), Math.cos(phi1) * Math.sin(phi2) -
			Math.sin(phi1) * Math.cos(phi2) * Math.cos(deltaLambda))

		val normalizedOrientation = orientation match {
			case o if o < -Math.PI => o + (Math.PI * 2)
			// ensure that the interval is open at the right end
			case o if o % (Math.PI * 2) == Math.PI => -Math.PI
			case o if o >= Math.PI * 2 => (o - Math.PI * 2) % (Math.PI * 2)
			case o if o >= Math.PI => o - (Math.PI * 2)
			case o => o
		}

		(length, normalizedOrientation)
	}

}

/**
 *
 */
class RoadSegment(val start: GHPoint3D, val end: GHPoint3D, val speedLimit: Double = 50 / 3.6) {

	def this(start: GHPoint3D, base: RoadSegment) {
		this(start, base.end, base.speedLimit)
		roadSign = base.roadSign
	}

	def this(base: RoadSegment, end: GHPoint3D) {
		this(base.start, end, base.speedLimit)
		roadSign = base.roadSign
	}


	// length is slightly inaccurate as we use a simplified formula for calculating it
	lazy val (length, orientation) = RoadSegment.getLengthAndOrientation(start, end)

	/**
	 * The grade of the road in rad
	 */
	lazy val grade = Math.atan((end.ele - start.ele) / length match {
		// ignore grade on short segments => it might lead to absurd results
		case x if length < 10.0 => 0.0
		case x if x == Double.NaN => 0.0
		// ignore grades greater than 35%; these are also unrealistic
		case x if x > 0.35 => 0.35
		case x if x < -0.35 => -0.35
		case x => x
	})


	var roadSign: Option[RoadSign] = None

	def setRoadSign(sign: RoadSign): Unit = roadSign = Some(sign)

	def setRoadSign(sign: Option[RoadSign]): Unit = roadSign = sign


	var roadName: Option[String] = None

	def setRoadName(name: String): Unit = roadName = Some(name)

	def setRoadName(name: Option[String]): Unit = roadName = name

	/**
	 * Checks if the given segment belongs to the same road as this one.
	 *
	 * This is a rather weak check as it just checks for the name that is assigned to the segments. It might give wrong
	 * results if segments in two areas are compared and both streets by chance have the same name. This is however pretty
	 * unlikely for our purpose, so we can live with that.
	 */
	def isOnSameRoadAs(other: RoadSegment): Boolean = {
		val names = List(roadName, other.roadName).flatten

		(!names.isEmpty && names.size == 2 && names.head == names.tail.head)
	}



	/**
	 * Returns the angle necessary to get from this segment to the given segment.
	 *
	 * The return value is confined to an interval [-pi..pi). The resulting orientation is always absolute (i.e. in
	 * [-pi..pi)) and not relative to the start of the journey.
	 */
	def calculateNecessaryTurn(nextSegment: RoadSegment): Double = {
		nextSegment.orientation - orientation match {
			case x if x >= Math.PI * 2 => x % Math.PI
				// TODO this will not suffice if the value is greater than 3 pi/smaller than -3pi
				// a formula to fix this could look like this: (x + ((Math.abs(x) / (Math.PI * 2)).floor * Math.PI * 2))
			case x if x >= Math.PI => x - Math.PI * 2
			case x if x < -Math.PI => x + Math.PI * 2
			case x => x
		}
	}

	override def toString = f"RoadSegment($length%.2f, ${orientation.toDegrees}%.2f°)"

	override def equals(obj: scala.Any): Boolean = {
		obj match {
			case segment: RoadSegment =>
				segment.start == start && segment.end == end
			case _ =>
				super.equals(obj)
		}
	}
}
