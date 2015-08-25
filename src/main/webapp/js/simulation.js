/*
 * Copyright (c) 2015 Andreas Wolf
 *
 * See te LICENSE file in the project root for further copyright information.
 */

var Simulation = function (id) {
	this.id = id;
};

(function (roadhopper) {

	/**
	 * The Leaflet.Playback instance used
	 */
	var playback = null;

	var $simulationDataContainer = null;

	var currentSimulation = null;

	Simulation.callbacks = [];

	Simulation.prototype.registerDataUpdateCallback = function(cb) {
		Simulation.callbacks.push(cb);
	};

	Simulation.prototype.checkStatus = function () {
		var sim = this;
		$.ajax({
			timeout: 30000,
			url: host + '/roadhopper/simulationstatus?id=' + sim.id,
			type: "GET",
			dataType: "json",
			success: function (json) {
				if (json["status"] == "finished") {
					$simulationDataContainer.find('.simulationstatus').text("Simulating… finished at "
							+ Math.round(json["time"] / 1000) + "s");

					// simulation has finished, draw data and don’t check again
					sim.updateSimulationData(json);
				} else {
					console.log(json["time"]);
					$simulationDataContainer.find('.simulationstatus').text("Simulating… "
							+ Math.round(json["time"] / 1000) + "s");

					// simulation has not finished, check again
					window.setTimeout(function() {sim.checkStatus();}, 2000);
				}
			},
			complete: function (xhr, status) {
				if (status != "success") {
					window.setTimeout(function() {sim.checkStatus();}, 2000);
				}
			}
		})
	};

	Simulation.prototype.updateSimulationData = function (JSONdata) {
		if (!playback) {
			playback = new TimeSeriesPlayback();
		}
		var timeSeries = new TimeSeriesDataSet(JSONdata["result"]);

		console.debug("Setting time series data for playback");
		playback.setData(timeSeries);

		for(var i = 0; i < Simulation.callbacks.length; ++i) {
			var callback = Simulation.callbacks[i];
			callback.apply(this, [timeSeries]);
		}
	};

	var createSimulateButton = function(routeId) {
		var button = $('<button class="simulate" />').text("Simulate");
		button.data("route", routeId);

		button.on("click", function () {
			console.debug("Starting simulation…");
			$.ajax({
				timeout: 30000,
				url: host + '/roadhopper/simulate?route=' + $(this).data("route"),
				type: "GET",
				dataType: "json",
				crossDomain: true,
				beforeSend: function () {
					$simulationDataContainer.empty().text("Running simulation…");
				},
				success: function (json) {
					if (typeof(json["simulation"]) == "undefined") {
						alert("No simulation ID received with response; see console for details");
						console.error("Server response for simulation request: ", json);
						return;
					}
					currentSimulation = new Simulation(json["simulation"]);
					$simulationDataContainer.empty().text("Simulation ID: " + currentSimulation.id);
					$simulationDataContainer.append('<div class="simulationstatus" />')
					currentSimulation.checkStatus();
				}
			});
		});

		return button;
	};

	$(function () {
		// the container for simulation-related content underneath the search form
		$simulationDataContainer = $('<div class="simulation" />');
		$('[data-module="simulation"] .content').append($simulationDataContainer);
	});

	function displaySimulationStartButton(JSONdata) {
		if (JSONdata["routeId"]) {
			$('[data-module="simulation"] .content .simulation').empty().append(createSimulateButton(JSONdata["routeId"]));
			currentSimulation = null;
		} else {
			alert("Did not get route ID; cannot simulate.");
		}
	}

	roadhopper.addRouteDrawCallback(displaySimulationStartButton);
})(graphHopperIntegration);
