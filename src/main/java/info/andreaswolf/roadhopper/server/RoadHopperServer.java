package info.andreaswolf.roadhopper.server;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;
import com.google.inject.servlet.GuiceFilter;
import com.graphhopper.http.DefaultModule;
import com.graphhopper.http.GHServer;
import com.graphhopper.http.GHServletModule;
import com.graphhopper.http.InvalidRequestServlet;
import com.graphhopper.util.CmdArgs;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.HandlerList;
import org.eclipse.jetty.server.handler.ResourceHandler;
import org.eclipse.jetty.server.nio.SelectChannelConnector;
import org.eclipse.jetty.servlet.FilterHolder;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.DispatcherType;
import java.util.EnumSet;


/**
 * (Preliminary) Main class for the HTTP-based RoadHopper service.
 *
 * This is a shameless copy of {@link GHServer}. Almost all praise should go to its developers. Blame for messing up
 * is on me…
 */
public class RoadHopperServer
{
	public static void main(String[] args) throws Exception
	{
		new RoadHopperServer(CmdArgs.read(args)).start();
	}

	private final CmdArgs args;
	private Server server;
	private final Logger logger = LoggerFactory.getLogger(getClass());

	public RoadHopperServer(CmdArgs args)
	{
		this.args = args;
	}

	public void start() throws Exception
	{
		Injector injector = Guice.createInjector(createModule());
		start(injector);
	}

	public void start(Injector injector) throws Exception
	{
		ResourceHandler resHandler = new ResourceHandler();
		resHandler.setDirectoriesListed(false);
		resHandler.setWelcomeFiles(new String[]
				{
						"index.html"
				});
		resHandler.setResourceBase(args.get("jetty.resourcebase", "./src/main/webapp"));

		server = new Server();
		// getSessionHandler and getSecurityHandler should always return null
		ServletContextHandler servHandler = new ServletContextHandler(ServletContextHandler.NO_SECURITY | ServletContextHandler.NO_SESSIONS);
		servHandler.setContextPath("/");

		servHandler.addServlet(new ServletHolder(new InvalidRequestServlet()), "/*");

		FilterHolder guiceFilter = new FilterHolder(injector.getInstance(GuiceFilter.class));
		servHandler.addFilter(guiceFilter, "/*", EnumSet.allOf(DispatcherType.class));

		SelectChannelConnector connector0 = new SelectChannelConnector();
		int httpPort = args.getInt("jetty.port", 8989);
		String host = args.get("jetty.host", "");
		connector0.setPort(httpPort);
		if (!host.isEmpty())
			connector0.setHost(host);

		server.addConnector(connector0);

		HandlerList handlers = new HandlerList();
		handlers.setHandlers(new Handler[]
				{
						resHandler, servHandler
				});
		server.setHandler(handlers);
		server.start();
		logger.info("Started server at HTTP " + host + ":" + httpPort);
	}

	protected Module createModule()
	{
		return new AbstractModule()
		{
			@Override
			protected void configure()
			{
				binder().requireExplicitBindings();

				install(new RoadHopperModule(args));
				install(new GHServletModule(args));
				install(new RoadHopperServletModule());

				bind(GuiceFilter.class);
			}
		};
	}

	public void stop()
	{
		if (server == null)
			return;

		try
		{
			server.stop();
		} catch (Exception ex)
		{
			logger.error("Cannot stop jetty", ex);
		}
	}

}
