package info.andreaswolf.roadhopper.route;

import com.graphhopper.routing.Path;

import java.util.List;

/**
 * A collection of paths that together form a route requested by a user.
 */
public class Route
{

	protected List<Path> paths;

	public Route(List<Path> paths) {
		this.paths = paths;
	}

	// TODO get a list of tower nodes for all paths/a single path
}
