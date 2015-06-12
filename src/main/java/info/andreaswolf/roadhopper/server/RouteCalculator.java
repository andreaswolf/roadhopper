package info.andreaswolf.roadhopper.server;

import com.graphhopper.GHRequest;
import com.graphhopper.routing.Path;
import com.graphhopper.routing.util.FlagEncoder;
import com.graphhopper.util.shapes.GHPoint;
import com.graphhopper.util.shapes.GHPoint3D;
import info.andreaswolf.roadhopper.RoadHopper;
import info.andreaswolf.roadhopper.road.Route;

import java.util.List;


public class RouteCalculator
{

	protected RoadHopper hopper;
	private String weighting, algorithm, locale;

	public RouteCalculator(RoadHopper hopper)
	{
		this.hopper = hopper;
	}

	public RouteCalculator setWeighting(String weighting)
	{
		this.weighting = weighting;
		return this;
	}

	public RouteCalculator setAlgorithm(String algorithm)
	{
		this.algorithm = algorithm;
		return this;
	}

	public RouteCalculator setLocale(String locale)
	{
		this.locale = locale;
		return this;
	}

	public List<Path> getPaths(String vehicle, List<GHPoint> infoPoints)
	{
		FlagEncoder algoVehicle = hopper.getEncodingManager().getEncoder(vehicle);
		GHRequest request = new GHRequest(infoPoints);

		//initHints(request, httpReq.getParameterMap());
		request.setVehicle(algoVehicle.toString()).
				setWeighting(weighting).
				setAlgorithm(algorithm).
				setLocale(locale).
				getHints()/*.
				put("calcPoints", calcPoints).
				put("instructions", enableInstructions).
				put("wayPointMaxDistance", minPathPrecision)*/;

		return hopper.getPaths(request);
	}

}
