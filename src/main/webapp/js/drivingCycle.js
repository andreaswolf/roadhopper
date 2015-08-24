/*
 * Copyright (c) 2015 Andreas Wolf
 *
 * See te LICENSE file in the project root for further copyright information.
 */

/**
 * Visualization library for driving cycles based on D3.js
 */
(function (roadhopper) {

	var margins = {top: 20, right: 15, bottom: 40, left: 40},
			height = 200,
			width = 500;

	var drivingCycle = {
		/**
		 * The driving cycle graph
		 */
		chart: null,

		margin: margins,
		height: 200 - margins.top - margins.bottom,
		width: 500 - margins.left - margins.right,

		draw: function (timeSeries) {
			// see http://bl.ocks.org/mbostock/1166403 for some of the inspiration used for this code
			var values = timeSeries.timestamps.map(function (timestamp) {
				return [timestamp, timeSeries.speedForTime(timestamp)];
			});

			// Scales and axes.
			var x = d3.scale.linear().range([0, this.width]),
					y = d3.scale.linear().range([this.height, 0]),
					xAxis = d3.svg.axis().scale(x).tickSize(-this.height).tickSubdivide(true),
					yAxis = d3.svg.axis().scale(y).ticks(4).orient("left");

			x.domain([values[0][0] / 1000, values[values.length - 1][0] / 1000]);
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

			// the axis labels
			this.graph.append("text")
					.attr("transform", "rotate(-90)")
					.attr("y", 0)
					.attr("x", 0 - (height / 2))
					.attr("dy", "15px")
					.style("text-anchor", "middle")
					.text("Speed [m/s]");
			this.graph.append("text")
					.attr("y", height)
					.attr("x", width / 2)
					.attr("dy", "-10px")
					.style("text-anchor", "middle")
					.text("Time [s]");

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


			var initialPosition = timeSeries.positionForTime(timeSeries.timestamps[0]);
			var positionMarker = L.circleMarker(new L.LatLng(initialPosition['lat'], initialPosition['lon']), {
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
							var currentTime = actualValue[0];
							var position = timeSeries.positionForTime(currentTime);
							positionMarker.setLatLng(new L.LatLng(position['lat'], position['lon']));
							focus.select("text").text(timeSeries.speedForTime(currentTime).toFixed(1) + " m/s ("
									+ (timeSeries.speedForTime(currentTime) * 3.6).toFixed(1) + " km/h)"
							);
							// TODO update text position if we are too far to the right
							focus.attr("transform", "translate(" + d3.mouse(this)[0] + "," + y(actualValue[1]) + ")");
						}
					});
		}
	};

	Simulation.prototype.registerDataUpdateCallback(function(timeSeries) {
		console.debug("Updating data");
		drivingCycle.draw.apply(drivingCycle, [timeSeries]);
	});

	return drivingCycle;

})(graphHopperIntegration);
