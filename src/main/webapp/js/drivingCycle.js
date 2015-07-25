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
					.data(values)
					// the chart canvas
					.append("g")
					.attr("width", this.width).attr("height", this.height)
				// position the basic chart canvas within the parent container
					.attr("transform", "translate(" + this.margin.left + "," + this.margin.top + ")");

			// the clip path; helps preventing stuff from leaking outside the chart canvas
			this.graph.append("clipPath")
					.attr("id", "clip")
					.append("rect")
					.attr("width", this.width)
					.attr("height", this.height);


			// the axes; need to be drawn before the actual graph elements, otherwise these would be shown behind the
			// axes/vertical help lines for the axes
			this.graph.append("g")
					.attr("class", "x axis")
					.attr("transform", "translate(0," + this.height + ")")
					.call(xAxis);
			this.graph.append("g")
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
			this.graph.append("path")
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
			this.graph.append("path")
					.attr("class", "area")
					.attr("clip-path", "url(#clip)")
					.attr("d", area(values));
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
