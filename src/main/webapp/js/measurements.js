(function (roadHopper, $) {

	/**
	 * The layers to display the measurement data on.
	 */
	var measuredRoad, measurementRoad;

	/**
	 * The map coordinates that were matched from the measurement.
	 */
	var matchedRoad = null;

	roadHopper.initMapCallbacks.push(function () {
		measuredRoad = L.geoJson().addTo(map);
		measuredRoad.options = {
			style: {color: "#0033cc", "weight": 5, "opacity": 0.6} // route color and style
		};
		measurementRoad = L.geoJson().addTo(map);
		measurementRoad.options = {
			style: {color: "#00cc33", "weight": 5, "opacity": 0.6} // route color and style
		};
	});


	var $groupList = $('<ul id="measurement-groups" />');
	var $group = $('<ul id="measurement-group" />');
	var $simulateButton = $('<button class="simulate">Simulate this</button>');

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

	var drawMeasurement = function (measurement, _matchedRoad) {
		var timeSeries = new TimeSeriesDataSet(measurement);
		matchedRoad = _matchedRoad;

		playback.setData(timeSeries);

		var geoJson = {
			"type": "Feature",
			"geometry": {
				"type": "LineString",
				"coordinates": Object.keys(measurement).map(function (key, index) {
					var measurementDatum = measurement[key];
					return [measurementDatum["position"]["lon"], measurementDatum["position"]["lat"]];
				})
			}
		};
		measuredRoad.clearLayers();
		measuredRoad.addData(geoJson);

		measurementRoad.clearLayers();
		if (matchedRoad instanceof Array) {
			measurementRoad.addData({
				type: "Feature",
				geometry: {
					type: "LineString",
					coordinates: matchedRoad
				}
			});
		}
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

	var loadMeasurement = function (name, $listItem) {
		console.info("Starting to load data set" + name);

		$listItem.css('font-style', 'italic');

		$.ajax({
			timeout: 30000,
			url: host + '/roadhopper/measurements?name=' + name,
			success: function (json) {
				var measurement = json["measurements"];
				var matchedRoad = json["matchedRoad"];
				drawMeasurement(measurement, matchedRoad);
			},
			error: function (err) {
				console.error("Error while fetching measurements", err);
			},
			complete: function() {
				$listItem.siblings('li').css('font-weight', 'normal').css('font-style', 'regular');
				$listItem.css('font-weight', 'bold').css('font-style', 'regular');
				$listItem.siblings('li').find('button').remove();
				$listItem.append($simulateButton);
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
			loadMeasurement($(this).data('measurement'), $(this));
		});
		$group.on('click', 'button.simulate', function() {
			console.debug("road:", matchedRoad);

			// TODO clear the routing layer

			var from = matchedRoad[0];
			var to = matchedRoad[matchedRoad.length - 1];
			initFromParams({
				point: [
					[from[1], from[0]],
					[to[1], to[0]]
				]
			});
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
