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

/**
 * Visualization library for driving cycles based on D3.js
 */
(function (roadhopper) {

	var margins = {top: 20, right: 30, bottom: 20, left: 15};
	var drivingCycle = {
		/**
		 * The driving cycle graph
		 */
		chart: null,

		margin: margins,
		height: 200 - margins.top - margins.bottom,
		width: 500 - margins.left - margins.right,

		draw: function (JSONdata) {
			// see http://bl.ocks.org/mbostock/1166403 for some of the inspiration used for this code
			var values = this.getVelocityOverTime(JSONdata["simulation"]);

			// Scales and axes.
			var x = d3.scale.linear().range([0, this.width]),
					y = d3.scale.linear().range([this.height, 0]),
					xAxis = d3.svg.axis().scale(x).tickSize(-this.height).tickSubdivide(true),
					yAxis = d3.svg.axis().scale(y).ticks(4).orient("right");

			x.domain([values[0][0], values[values.length - 1][0] / 1000]);
			y.domain([0, d3.max(values, function (d) {
				return d[1];
			})]).nice();

			// the parent container
			var container = d3.select("#map").append("div")
					.attr("class", "driving-cycle");
			// the chart SVG element
			this.graph = container.append("svg")
					.attr("class", "d3-diagram")
					.data(values);

			// the chart canvas
			var chartCanvas = this.graph.append("g")
					.attr("width", this.width).attr("height", this.height)
				// position the basic chart canvas within the parent container
					.attr("transform", "translate(" + this.margin.left + "," + this.margin.top + ")");

			// the clip path; helps preventing stuff from leaking outside the chart canvas
			chartCanvas.append("clipPath")
					.attr("id", "clip")
					.append("rect")
					.attr("width", this.width)
					.attr("height", this.height);


			// the axes; need to be drawn before the actual graph elements, otherwise these would be shown behind the
			// axes/vertical help lines for the axes
			chartCanvas.append("g")
					.attr("class", "x axis")
					.attr("transform", "translate(0," + this.height + ")")
					.call(xAxis);
			chartCanvas.append("g")
					.attr("class", "y axis")
					.attr("transform", "translate(" + this.width + ",0)")
					.call(yAxis);


			// the actual chart line
			var line = d3.svg.line()
					.interpolate("monotone")
					.x(function (d) {
						return x(d[0] / 1000);
					})
					.y(function (d) {
						return y(d[1]);
					});
			// the line path
			chartCanvas.append("path")
					.attr("class", "line")
					.attr("clip-path", "url(#clip)")
					.attr("d", line(values));

			// the area below the line
			var area = d3.svg.area()
					.interpolate("monotone")
					.x(function (d) {
						return x(d[0] / 1000);
					})
					.y0(this.height)
					.y1(function (d) {
						return y(d[1]);
					});
			chartCanvas.append("path")
					.attr("class", "area")
					.attr("clip-path", "url(#clip)")
					.attr("d", area(values));


			var position = JSONdata["simulation"]["0"]["position"];
			var positionMarker = L.circleMarker(new L.LatLng(position['lat'], position['lon']), {
				clickable: false
			});

			// the marker at the currently selected datum
			var focus = chartCanvas.append("g")
					.attr("class", "focus")
					.style("display", null);
			focus.append("circle")
					.attr("r", 4.5);
			focus.append("text")
					.attr("x", 9)
					.attr("dy", ".35em");

			var bisectValues = d3.bisector(function (d) {
				return d[0];
			}).left;
			// the overlay for the graph for displaying tooltips. We need a separate layer for that so we have a
			// completely sensitive area; otherwise, only the visible/drawn parts of the used layer would be sensitive
			// to mouse movements
			var overlay = this.graph.append("rect")
					.attr("class", "overlay")
					.attr("width", this.width).attr("height", this.height)
					.attr("transform", "translate(" + this.margin.left + "," + this.margin.top + ")")
					.on("mouseover", function () {
						focus.style("display", null);
						positionMarker.addTo(map);
					})
					.on("mouseout", function () {
						focus.style("display", "none");
						map.removeLayer(map);
					})
					.on("mousemove", function (e) {
						// cf. http://bl.ocks.org/mbostock/3902569
						// we get a raw value from x.invert, so we first need to find the correct value by bisecting the
						// values array. The returned index might point to either the left or right value, so we need
						// to check which is nearer
						var interpolatedTime = x.invert(d3.mouse(this)[0]) * 1000,
								i = bisectValues(values, interpolatedTime, 1),
								leftValue = values[i - 1],
								rightValue = values[i],
								actualValue = (interpolatedTime - leftValue[0] > rightValue[0] - interpolatedTime)
										? rightValue : leftValue;
						if (typeof(actualValue) != "undefined") {
							var dataEntry = JSONdata["simulation"][actualValue[0]];
							var position = dataEntry["position"];
							positionMarker.setLatLng(new L.LatLng(position['lat'], position['lon']));
							focus.select("text").text(dataEntry["speed"].toFixed(1) + " m/s ("
									+ (dataEntry["speed"] * 3.6).toFixed(1) + " km/h)"
							);
							// TODO update text position if we are too far to the right
							focus.attr("transform", "translate(" + d3.mouse(this)[0] + "," + y(actualValue[1]) + ")");
						}
					});
		},

		getVelocityOverTime: function (data) {
			var points = [];
			for (var time in data) {
				if (data.hasOwnProperty(time)) {
					points.push([time, data[time]["speed"]]);
				}
			}
			return points;
		}
	};

	roadhopper.addRouteDrawCallback(drivingCycle.draw, drivingCycle);

	return drivingCycle;

})(graphHopperIntegration);
