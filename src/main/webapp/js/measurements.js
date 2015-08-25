(function (roadHopper, $) {

	var measurementRoad;

	roadHopper.initMapCallbacks.push(function () {
		measurementRoad = L.geoJson().addTo(map);
		measurementRoad.options = {
			style: {color: "#00cc33", "weight": 5, "opacity": 0.6} // route color and style
		};
	});
	var playback = new TimeSeriesPlayback();
	var running = false;
	// TODO this callback must not be registered twice!
	playback.registerCallback(function(time) {
		if (!playback.timeSeries.hasTime(time)) {
			return;
		}
		if (!running) {
			// TODO we cannot detect the end of a playback currently
			running = true;
			$('#vehicle-status').show();
		}
		$("#vehicle-status").find('[data-index="speed"]').text(playback.timeSeries.speedForTime(time).toFixed(2));
	});

	var drawMeasurement = function (json) {
		var timeSeries = new TimeSeriesDataSet(json);

		playback.setData(timeSeries);

		var geoJson = {
			"type": "Feature",
			"geometry": {
				"type": "LineString",
				"coordinates": Object.keys(json).map(function (key, index) {
					var measurementDatum = json[key];
					return [measurementDatum["position"]["lon"], measurementDatum["position"]["lat"]];
				})
			}
		};
		console.debug(geoJson);
		measurementRoad.clearLayers();
		measurementRoad.addData(geoJson);
	};

	var fetchMeasurement = function () {
		var fileName = $(this).data('name');
		console.info("Starting to load data set" + fileName);
		$('#measurement-file-indicator').text('Loading file ' + fileName + '…');

		$.ajax({
			timeout: 30000,
			url: host + '/roadhopper/measurements?name=' + fileName,
			success: function (json) {
				$('#measurement-files').hide();
				$('#measurement-file-indicator').text('Loaded file ' + fileName);
				var measurements = json["measurements"];
				console.debug(typeof( measurements));
				if (measurements instanceof Array) {
					var measurementList = $('<ul />');
					for (var i = 0; i < measurements.length; ++i) {
						// Get the length of the measurement
						var keys = Object.keys(measurements[i]);
						var length = ((keys[keys.length - 1] - keys[0]) / 1000).toFixed(0);

						measurementList.append($('<li data-id="' + i + '">Measurement ' + i + ' (' + length + 's)' + '</li>'))
					}
					measurementList.on('click', 'li', function() {
						var i = $(this).data('id');
						$(this).siblings('li').css('font-weight', 'normal');
						$(this).css('font-weight', 'bold');
						console.debug(i);
						drawMeasurement(measurements[i]);
					});
					$('#measurement-file-indicator').append(measurementList);
				}
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
