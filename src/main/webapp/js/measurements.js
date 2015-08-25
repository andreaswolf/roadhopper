(function (roadHopper, $) {

	var measurementRoad;

	roadHopper.initMapCallbacks.push(function () {
		measurementRoad = L.geoJson().addTo(map);
		measurementRoad.options = {
			style: {color: "#00cc33", "weight": 5, "opacity": 0.6} // route color and style
		};
	});

	var drawMeasurement = function (json) {
		var timeSeries = new TimeSeriesDataSet(json["measurements"]);
		var playback = new TimeSeriesPlayback();

		playback.setData(timeSeries);

		var running = false;
		// TODO this callback must not be registered twice!
		playback.registerCallback(function(time) {
			if (!timeSeries.hasTime(time)) {
				return;
			}
			if (!running) {
				// TODO we cannot detect the end of a playback currently
				running = true;
				$('#vehicle-status').show();
			}
			$("#vehicle-status").find('[data-index="speed"]').text(timeSeries.speedForTime(time).toFixed(2));
		});

		var geoJson = {
			"type": "Feature",
			"geometry": {
				"type": "LineString",
				"coordinates": json["road"]["segments"].map(function (segment) {
					return [segment["lon"], segment["lat"]];
				})
			}
		};
		measurementRoad.addData(geoJson);
	};

	var fetchMeasurement = function () {
		var measurement = $(this).data('name');
		console.info("Starting to load data set" + measurement);
		$('#measurement-file-indicator').text('Loading file ' + measurement + '…');

		$.ajax({
			timeout: 30000,
			url: host + '/roadhopper/measurements?name=' + measurement,
			success: function (json) {
				drawMeasurement(json);
				$('#measurement-files').hide();
				$('#measurement-file-indicator').text('Loaded file ' + measurement);
			},
			error: function (err) {
				console.error("Error while fetching measurements", err);
			},
			type: "GET",
			dataType: "json",
			crossDomain: true
		});
	};

	/**
	 * Constructor for the module. Creates the module contents and registers it with the roadhopper module.
	 */
	(function () {
		var $fileList = $('<ul id="measurement-files" />');
		var $moduleContents = $('<div id="measurement-file-indicator" />').append($fileList);

		roadHopper.addModule("measurements", "Measurements", $moduleContents);
		$fileList.on('click', 'li', fetchMeasurement);

		$.ajax({
			timeout: 30000,
			url: host + '/roadhopper/measurements',
			type: "GET",
			dataType: "json",
			crossDomain: true,
			success: function (json) {
				var files = json["files"];
				for (var i = 0; i < files.length; ++i) {
					$fileList.append($('<li data-name="' + files[i] + '">' + files[i] + '</li>'));
				}
			}
		});
	})();

})(graphHopperIntegration, jQuery);
