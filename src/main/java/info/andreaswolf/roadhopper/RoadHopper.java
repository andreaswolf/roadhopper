package info.andreaswolf.roadhopper;

import com.graphhopper.GHRequest;
import com.graphhopper.GHResponse;
import com.graphhopper.GraphHopper;
import com.graphhopper.routing.Path;
import info.andreaswolf.roadhopper.route.Route;

import java.util.List;

public class RoadHopper extends GraphHopper
{

	public Route createRoute(GHRequest request) {
		return new Route(getPaths(request));
	}

	public List<Path> getPaths(GHRequest request)
	{
		GHResponse response = new GHResponse();
		List<Path> paths = getPaths(request, response);

		return paths;
	}

}
