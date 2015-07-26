/*
 * Copyright (c) 2015 Andreas Wolf
 *
 * Permission is hereby granted, free of charge, to any person obtaining
 * a copy of this software and associated documentation files (the "Software"),
 * to deal in the Software without restriction, including without limitation
 * the rights to use, copy, modify, merge, publish, distribute, sublicense,
 * and/or sell copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included
 * in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS
 * OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

(function (roadhopper) {

	var simulation = {
		drawVehicleMarker: function (JSONdata) {
			var markerIcon = L.icon({
				iconUrl: './img/marker-36.png',
				iconAnchor: [18, 18],
				iconSize: [36, 36]
			});

			var simulationDataGeoJson = this.convertSimulationDataToGeoJson(JSONdata["simulation"]);

			var playback = new L.Playback(map, simulationDataGeoJson, null, {
				marker: function () {
					return {
						icon: markerIcon,
						iconAngle: 90
					}
				}
			});
			playback.addCallback(function(timestamp) {
				if (!simulationDataGeoJson["data"].hasOwnProperty(timestamp.toString())) {
					console.error("No direction for " + timestamp);
					return;
				}
				var angle = simulationDataGeoJson["data"][timestamp.toString()]["direction"] * 180 / Math.PI;

				// ATTENTION: This is an undocumented and unsupported hack to get the marker as it is not exposed via
				// the official API.
				playback._trackController._tracks[0]._marker.setIconAngle(angle);
			});

			// Initialize custom control
			var control = new L.Playback.Control(playback);
			control.addTo(map);
		},

		convertSimulationDataToGeoJson: function (data) {
			var timestamps = [], coordinates = [], direction = {};
			for (var time in data) {
				if (data.hasOwnProperty(time)) {
					// if any data point is undefined, the control apparently enters an uncontrolled endless loop
					if (data[time]["position"]["lon"] == undefined) {
						continue;
					}
					timestamps.push(parseInt(time));
					coordinates.push([data[time]["position"]["lon"], data[time]["position"]["lat"]]);
				}
			}

			return {
				"type": "Feature",
				"geometry": {
					"type": "MultiPoint",
					"coordinates": coordinates
				},
				"properties": {
					"time": timestamps
				},
				// The data as received from the server; used for processing further information
				data: data
			}
		}
	};

	roadhopper.addRouteDrawCallback(simulation.drawVehicleMarker, simulation);
})(graphHopperIntegration);
