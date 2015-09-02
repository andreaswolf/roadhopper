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
		_updatePoints: function () {
			this.points = [this.start].concat(this.intermediate, [this.end]);
			this.$rootScope.$broadcast('routePointsUpdated');
		}
	});

	return Route;
});
