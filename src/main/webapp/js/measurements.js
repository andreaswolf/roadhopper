(function (roadHopper, $) {
	var drawMeasurement = function (json) {
		var timeSeries = new TimeSeriesDataSet(json["measurements"]);
		var playback = new TimeSeriesPlayback();

		playback.setData(timeSeries);
	};

	var fetchMeasurement = function () {
		var measurement = $(this).data('name');
		console.info("Starting to load data set" + measurement);

		$.ajax({
			timeout: 30000,
			url: host + '/roadhopper/measurements?name=' + measurement,
			success: function (json) {
				drawMeasurement(json);
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
		var $moduleContents = $('<ul id="measurement-files">');
		$moduleContents.append($('<li data-name="Dienstag_1">foo</li>'));
		$moduleContents.on('click', 'li', fetchMeasurement);

		roadHopper.addModule("measurements", "Measurements", $moduleContents);
	})();

})(graphHopperIntegration, jQuery);
