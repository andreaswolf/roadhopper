/*
 * Copyright (c) 2015 Andreas Wolf
 *
 * See te LICENSE file in the project root for further copyright information.
 */

(function (roadhopper) {

	var simulation = {
		markerDrawn: false,

		/**
		 * The Leaflet.Playback instance used
		 */
		playback: null,

		/**
		 * The vehicle position marker
		 */
		positionMarker: null,
		timeSeries: null,
		data: null,

		drawVehicleMarker: function (JSONdata) {
			var markerIcon = L.icon({
				iconUrl: './img/marker-36.png',
				iconAnchor: [18, 18],
				iconSize: [36, 36]
			});

			this.timeSeries = new TimeSeriesDataSet(JSONdata["simulation"]);
			this.data = this.convertTimeSeriesToGeoJson(this.timeSeries);

			if (!this.markerDrawn) {
				this.playback = new L.Playback(map, this.data, null, {
					marker: function () {
						return {
							icon: markerIcon,
							iconAngle: 90
						}
					}
				});
				this.playback.addCallback(function(timestamp) {
					if (!simulation.timeSeries.hasTime(timestamp)) {
						console.error("No direction for " + timestamp);
						return;
					}
					var angle = simulation.timeSeries.directionForTime(timestamp);

					// ATTENTION: This is an undocumented and unsupported hack to get the marker as it is not exposed via
					// the official API.
					simulation.playback._trackController._tracks[0]._marker.setIconAngle(angle);
				});
				this.markerDrawn = true;

				// Initialize custom control
				this.positionMarker = new L.Playback.Control(this.playback);
				this.positionMarker.addTo(map);
			} else {
				this.playback.clearData();
				this.playback.setData(simulation.data);
			}
		},

		convertTimeSeriesToGeoJson: function (timeSeries) {
			return {
				"type": "Feature",
				"geometry": {
					"type": "MultiPoint",
					"coordinates": timeSeries.coordinates
				},
				"properties": {
					"time": timeSeries.timestamps
				},
				// The data as received from the server; used for processing further information
				data: timeSeries.data
			}
		}
	};

	roadhopper.addRouteDrawCallback(simulation.drawVehicleMarker, simulation);
})(graphHopperIntegration);
