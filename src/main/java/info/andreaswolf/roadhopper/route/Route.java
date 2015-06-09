package info.andreaswolf.roadhopper.route;

import com.graphhopper.routing.Path;
import com.graphhopper.util.EdgeIteratorState;
import gnu.trove.list.TIntList;
import gnu.trove.list.array.TIntArrayList;

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

	public List<Path> getPaths()
	{
		return paths;
	}

	public TIntList getTowerNodeIds() {
		TIntList nodes = new TIntArrayList();
		for (Path path : paths) {
			for (EdgeIteratorState edge: path.calcEdges()) {
				nodes.add(edge.getBaseNode());
			}
		}

		return nodes;
	}
	// TODO get a list of tower nodes for all paths/a single path
}
