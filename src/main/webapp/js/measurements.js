(function (roadHopper, $) {

	var measurementRoad;

	roadHopper.initMapCallbacks.push(function () {
		measurementRoad = L.geoJson().addTo(map);
		measurementRoad.options = {
			style: {color: "#00cc33", "weight": 5, "opacity": 0.6} // route color and style
		};
	});


	var $groupList = $('<ul id="measurement-groups" />');
	var $group = $('<ul id="measurement-group" />');

	var measurementGroups = [];


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

	var renderGroups = function() {
		for (var file in measurementGroups) {
			if (measurementGroups.hasOwnProperty(file)) {
				$groupList.append($('<li data-group="' + file + '">' + file + '</li>'));
			}
		}
		$groupList.show();
		$group.hide();
	};

	var showGroup = function() {
		var groupName = $(this).data('group');

		var measurements = measurementGroups[groupName];

		$group.empty();
		for (var i = 0; i < measurements.length; ++i) {
			$group.append($('<li data-measurement="' + measurements[i] + '">' + measurements[i] + '</li>'));
		}
		$groupList.hide();
		$group.show();
	};

	var loadMeasurement = function (name) {
		console.info("Starting to load data set" + name);
		$('#measurement-file-indicator').text('Loading file ' + name + '…');

		$group.hide();
		$.ajax({
			timeout: 30000,
			url: host + '/roadhopper/measurements?name=' + name,
			success: function (json) {
				$('#measurement-file-indicator').text('Loaded file ' + name);
				var measurements = json["measurements"];
				drawMeasurement(measurements);
			},
			error: function (err) {
				console.error("Error while fetching measurements", err);
			},
			complete: function() {
				$group.hide();
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
		var $moduleContents = $('<div id="measurement-file-indicator" />').append($groupList).append($group);
		roadHopper.addModule("measurements", "Measurements", $moduleContents);
		$groupList.on('click', 'li', showGroup);
		$group.on('click', 'li', function() {
			loadMeasurement($(this).data('measurement'));
		});

		$.ajax({
			timeout: 30000,
			url: host + '/roadhopper/measurements',
			type: "GET",
			dataType: "json",
			crossDomain: true,
			success: function (json) {
				measurementGroups = json["files"];
				renderGroups();
			}
		});
	})();

})(graphHopperIntegration, jQuery);
