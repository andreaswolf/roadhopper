package info.andreaswolf.roadhopper.server;

import com.graphhopper.GHRequest;
import com.graphhopper.GHResponse;
import com.graphhopper.http.GraphHopperServlet;
import com.graphhopper.routing.util.FlagEncoder;
import com.graphhopper.storage.NodeAccess;
import com.graphhopper.storage.extensions.RoadSignEncoder;
import com.graphhopper.util.PointList;
import com.graphhopper.util.StopWatch;
import com.graphhopper.util.shapes.GHPoint;
import gnu.trove.procedure.TIntProcedure;
import info.andreaswolf.roadhopper.RoadHopper;
import info.andreaswolf.roadhopper.route.Route;
import org.json.JSONObject;

import javax.inject.Inject;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static javax.servlet.http.HttpServletResponse.SC_BAD_REQUEST;
import static javax.servlet.http.HttpServletResponse.SC_INTERNAL_SERVER_ERROR;

public class RoadHopperServlet extends GraphHopperServlet
{
	@Inject
	private RoadHopper hopper;

	@Override
	public void doGet(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException
	{
		// TODO this code is copied from GraphHopperServlet
		try
		{
			doRoute(req, res);
		} catch (IllegalArgumentException ex)
		{
			writeError(res, SC_BAD_REQUEST, ex.getMessage());
		} catch (Exception ex)
		{
			logger.error("Error while executing request: " + req.getQueryString(), ex);
			writeError(res, SC_INTERNAL_SERVER_ERROR, "Problem occured:" + ex.getMessage());
		}
	}

	protected void doRoute(HttpServletRequest httpRequest, HttpServletResponse httpResponse) throws Exception
	{
		// TODO this is mostly copied from GraphHopperServlet
		List<GHPoint> infoPoints = getPoints(httpRequest, "point");

		// we can reduce the path length based on the maximum differences to the original coordinates
		double minPathPrecision = getDoubleParam(httpRequest, "way_point_max_distance", 1d);
		boolean writeGPX = "gpx".equalsIgnoreCase(getParam(httpRequest, "type", "json"));
		boolean enableInstructions = writeGPX || getBooleanParam(httpRequest, "instructions", true);
		boolean calcPoints = getBooleanParam(httpRequest, "calc_points", true);
		boolean enableElevation = getBooleanParam(httpRequest, "elevation", false);
		boolean pointsEncoded = getBooleanParam(httpRequest, "points_encoded", true);

		String vehicleStr = getParam(httpRequest, "vehicle", "car");
		String weighting = getParam(httpRequest, "weighting", "fastest");
		String algoStr = getParam(httpRequest, "algorithm", "");
		String localeStr = getParam(httpRequest, "locale", "en");

		StopWatch sw = new StopWatch().start();
		GHResponse ghRsp;
		Route route = null;
		if (!hopper.getEncodingManager().supports(vehicleStr))
		{
			ghRsp = new GHResponse().addError(new IllegalArgumentException("Vehicle not supported: " + vehicleStr));
		} else if (enableElevation && !hopper.hasElevation())
		{
			ghRsp = new GHResponse().addError(new IllegalArgumentException("Elevation not supported!"));
		} else
		{
			FlagEncoder algoVehicle = hopper.getEncodingManager().getEncoder(vehicleStr);
			GHRequest request = new GHRequest(infoPoints);

			initHints(request, httpRequest.getParameterMap());
			request.setVehicle(algoVehicle.toString()).
					setWeighting(weighting).
					setAlgorithm(algoStr).
					setLocale(localeStr).
					getHints().
					put("calcPoints", calcPoints).
					put("instructions", enableInstructions).
					put("wayPointMaxDistance", minPathPrecision);

			ghRsp = hopper.route(request);
			route = hopper.createRoute(request);
		}

		float took = sw.stop().getSeconds();
		String infoStr = httpRequest.getRemoteAddr() + " " + httpRequest.getLocale() + " " + httpRequest.getHeader("User-Agent");
		String logStr = httpRequest.getQueryString() + " " + infoStr + " " + infoPoints + ", took:"
				+ took + ", " + algoStr + ", " + weighting + ", " + vehicleStr;

		if (ghRsp.hasErrors())
			logger.error(logStr + ", errors:" + ghRsp.getErrors());
		else
			logger.info(logStr + ", distance: " + ghRsp.getDistance()
					+ ", time:" + Math.round(ghRsp.getTime() / 60000f)
					+ "min, points:" + ghRsp.getPoints().getSize() + ", debug - " + ghRsp.getDebugInfo());

		if (writeGPX)
		{
			writeResponse(httpResponse, createGPXString(httpRequest, httpResponse, ghRsp));
		} else
		{
			Map<String, Object> map = createJson(ghRsp,
					calcPoints, pointsEncoded, enableElevation, enableInstructions);

			if (route != null)
			{
				// TODO enrich response with more information;
				new TrafficSignEnricher().enrich(map, route);
			}

			Object infoMap = map.get("info");
			if (infoMap != null)
				((Map) infoMap).put("took", Math.round(took * 1000));

			writeJson(httpRequest, httpResponse, new JSONObject(map));
		}

	}

	@Override
	protected Object createPoints(PointList points, boolean pointsEncoded, boolean includeElevation)
	{
		if (pointsEncoded)
		{
			return super.createPoints(points, pointsEncoded, includeElevation);
		}
		Map<String, Object> jsonPoints = (Map<String, Object>) super.createPoints(points, pointsEncoded, includeElevation);

		// TODO iterate over points, get
		jsonPoints.put("foo", "bar");

		return jsonPoints;
	}

	protected class TrafficSignEnricher {
		public void enrich(final Map<String, Object> responseContents, Route route) {
			final NodeAccess nodeAccess = hopper.getGraph().getNodeAccess();
			final List<PointInfo> towerNodeInfo = new ArrayList<PointInfo>(10);
			final RoadSignEncoder signEncoder = new RoadSignEncoder(hopper.getGraph());

			route.getTowerNodeIds().forEach(new TIntProcedure()
			{
				public boolean execute(int value)
				{
					String iconType = "";
					if (signEncoder.hasTrafficLight(value)) {
						iconType = "trafficLight";
					}
					towerNodeInfo.add(new PointInfo(nodeAccess.getLat(value), nodeAccess.getLon(value), iconType));
					return true;
				}
			});

			responseContents.put("towerNodes", towerNodeInfo);
		}
	}

	/**
	 *
	 */
	protected class PointInfo extends GHPoint {

		protected String info;

		public PointInfo(double lat, double lon, String info)
		{
			super(lat, lon);
			this.info = info;
		}

		public String getInfo()
		{
			return info;
		}
	}
}
