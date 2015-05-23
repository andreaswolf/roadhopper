package info.andreaswolf.roadhopper.server;

import com.graphhopper.http.GHBaseServlet;
import com.graphhopper.routing.Path;
import com.graphhopper.storage.NodeAccess;
import com.graphhopper.util.EdgeIteratorState;
import com.graphhopper.util.shapes.GHPoint;
import info.andreaswolf.roadhopper.RoadHopper;

import javax.inject.Inject;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class RoadProfileServlet extends GHBaseServlet
{
	@Inject
	private RoadHopper hopper;

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException
	{
		resp.getWriter().println("Hallo Welt");
		List<GHPoint> infoPoints = getPoints(req, "point");

		RouteCalculator calculator = new RouteCalculator(hopper);

		// TODO make parameters configurable
		calculator.setAlgorithm("").setWeighting("fastest").setLocale("de");
		List<Path> points = calculator.getPaths("car", infoPoints);

		NodeAccess nodeAccess = hopper.getGraph().getNodeAccess();

		int node;
		for (Path path : points)
		{
			resp.getWriter().println(path.toString());
			for (EdgeIteratorState edge : path.calcEdges()) {
				resp.getWriter().println("  edge: " + edge.getName() + ", dist: " + edge.getDistance() + ", flags: " + edge.getFlags());
				node = edge.getBaseNode();
				resp.getWriter().print("    " + nodeAccess.getLat(node) + " - " + nodeAccess.getLon(node));
				node = edge.getAdjNode();
				resp.getWriter().println(" -> " + nodeAccess.getLat(node) + " - " + nodeAccess.getLon(node));

				for (GHPoint point : edge.fetchWayGeometry(3)) {
					resp.getWriter().println("      " + point.getLat() + " - " + point.getLon());
				}
			}
		}
	}

	// TODO this is copied from GraphHopperServlet -> refactor it to its own class
	protected List<GHPoint> getPoints(HttpServletRequest req, String key)
	{
		String[] pointsAsStr = getParams(req, key);
		final List<GHPoint> infoPoints = new ArrayList<GHPoint>(pointsAsStr.length);
		for (String str : pointsAsStr)
		{
			String[] fromStrs = str.split(",");
			if (fromStrs.length == 2)
			{
				GHPoint point = GHPoint.parse(str);
				if (point != null)
				{
					infoPoints.add(point);
				}
			}
		}

		return infoPoints;
	}
}
