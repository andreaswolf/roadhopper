package info.andreaswolf.roadhopper.server;

import com.graphhopper.util.EdgeIteratorState;
import com.graphhopper.util.PointList;
import info.andreaswolf.roadhopper.road.RoadBend;
import info.andreaswolf.roadhopper.road.RoadSegment;
import info.andreaswolf.roadhopper.road.RoadSign;
import info.andreaswolf.roadhopper.road.Route;
import scala.collection.JavaConversions;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Encodes various objects to GeoJSON-compatible structures. The returned HashMaps can directly be serialized
 * to JSON using {@link org.json.JSONObject}
 */
class GeoJsonEncoder
{

	public List<Object> encodeRoute(Route hopperRoute)
	{
		// The list of points/road segments that make up the route
		List<Object> pointList = new ArrayList<Object>(10);
		for (RoadSegment part : JavaConversions.asJavaCollection(hopperRoute.parts()))
		{
			HashMap<String, Object> partInfo = new HashMap<String, Object>();
			encodeRoadSegment(partInfo, part, false);

			// road signs are by convention always added to the end of the road segment that precedes them
			// (see RouteFactory)
			if (!part.roadSign().isEmpty())
			{
				encodeRoadSign(pointList, part.roadSign().get());
			}

			pointList.add(partInfo);
		}
		return pointList;
	}

	public Object encodeEdge(EdgeIteratorState edge)
	{
		PointList points = edge.fetchWayGeometry(3);

		// TODO use 3D parameter
		return points.toGeoJson();
	}

	public void encodeRoadSegment(HashMap<String, Object> partInfo, RoadSegment segment, Boolean includeElevation)
	{
		ArrayList<Double[]> points = new ArrayList<Double[]>(2);

		// NOTE: GeoJSON uses lon/lat coordinates instead of lat/lon!
		points.add(segment.start().toGeoJson());
		points.add(segment.end().toGeoJson());

		partInfo.put("type", "LineString");
		partInfo.put("coordinates", points);
		partInfo.put("length", segment.length());
		partInfo.put("orientation", segment.orientation());
		partInfo.put("grade", segment.grade());
		if (segment.roadName().isDefined()) {
			partInfo.put("road", segment.roadName().get());
		} else {
			partInfo.put("road", "");
		}
		partInfo.put("speedLimit", segment.speedLimit());
	}

	public void encodeRoadSign(List<Object> pointList, RoadSign sign)
	{
		HashMap<String, Object> partInfo = new HashMap<String, Object>();

		partInfo.put("type", "Point");
		partInfo.put("info", sign.typeInfo());
		partInfo.put("id", sign.id());
		partInfo.put("coordinates", sign.coordinates().toGeoJson());

		pointList.add(partInfo);
	}

	public void encodeRoadBend(HashMap<String, Object> partInfo, RoadBend bend)
	{
		partInfo.put("type", "Point");
		partInfo.put("info", "RoadBend");
		partInfo.put("length", bend.length());
		partInfo.put("angle", bend.angle());
		partInfo.put("radius", bend.radius());
		partInfo.put("direction", bend.direction().id());
		partInfo.put("initialOrientation", bend.firstSegment().orientation());
		partInfo.put("coordinates", bend.firstSegment().start().toGeoJson());
	}
}
