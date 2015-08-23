/*
 * Copyright (c) 2015 Andreas Wolf
 *
 * See te LICENSE file in the project root for further copyright information.
 */

(function (roadhopper) {

	/**
	 * The Leaflet.Playback instance used
	 */
	var playback = null;

	function updateSimulationData(JSONdata) {
		if (!playback) {
			playback = new TimeSeriesPlayback();
		}
		var timeSeries = new TimeSeriesDataSet(JSONdata["simulation"]);

		console.debug("Setting time series data for playback");
		playback.setData(timeSeries);
	}

	roadhopper.addRouteDrawCallback(updateSimulationData);
})(graphHopperIntegration);
