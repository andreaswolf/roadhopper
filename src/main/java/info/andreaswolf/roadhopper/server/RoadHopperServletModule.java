package info.andreaswolf.roadhopper.server;

import javax.inject.Singleton;

public class RoadHopperServletModule extends com.google.inject.servlet.ServletModule
{

	@Override
	protected void configureServlets()
	{
		serve("/road").with(RoadProfileServlet.class);
		bind(RoadProfileServlet.class).in(Singleton.class);

		serve("/roadhopper/route").with(RoadHopperServlet.class);
		bind(RoadHopperServlet.class).in(Singleton.class);

		serve("/roadhopper/simulate").with(SimulationServlet.class);
		bind(SimulationServlet.class).in(Singleton.class);

		serve("/roadhopper/measurements").with(MeasurementsServlet.class);
		bind(MeasurementsServlet.class).in(Singleton.class);
	}
}
