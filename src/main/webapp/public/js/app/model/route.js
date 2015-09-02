/*
 * Copyright (c) 2015 Andreas Wolf
 *
 * See te LICENSE file in the project root for further copyright information.
 */

define(['underscore'], function (_) {

	/**
	 * Abstraction for a route.
	 *
	 * @constructor
	 */
	function Route($rootScope) {
		var attrs = {
			$rootScope: $rootScope,
			start: null,
			end: null,
			intermediate: [],
			points: []
		};

		_.extend(this, attrs);
	}

	_.extend(Route.prototype, {
		setStart: function (pos) {
			this.start = pos;
			this._updatePoints();
		},
		setEnd: function (pos) {
			this.end = pos;
			this._updatePoints();
		},
		addIntermediate: function (pos) {
			this.intermediate.push(pos);
			this._updatePoints();
		},
		isRoutable: function() {
			return this.start && this.end;
		},
		/**
		 *
		 * @param index
		 * @param coordinates LatLng
		 */
		movePoint: function(index, coordinates) {
			if (index == 0) {
				this.start.latlng = coordinates;
			} else if (index == this.intermediate.length + 1) {
				this.end.latlng = coordinates;
			} else {
				this.intermediate[index - 1].latlng = coordinates;
			}
			this._updatePoints();
		},
		_updatePoints: function () {
			this.points = [this.start].concat(this.intermediate, [this.end]);
			this.$rootScope.$broadcast('routePointsUpdated');
		}
	});

	return Route;
});
