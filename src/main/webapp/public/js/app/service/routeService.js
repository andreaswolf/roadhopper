/*
 * Copyright (c) 2015 Andreas Wolf
 *
 * See te LICENSE file in the project root for further copyright information.
 */
define(['app/base', 'underscore', 'app/model/route'], function (app, _, Route) {

	var RouteService = function ($rootScope) {
		console.debug("Creating route service");
		this.route = new Route($rootScope);
	};
	RouteService.prototype.setStartCoord = function (pos) {
		this.route.setStart(pos);
		this.checkCoordinatesAndDoRequest();
	};
	RouteService.prototype.addIntermediateCoord = function (pos) {
		this.route.addIntermediate(pos);
		this.checkCoordinatesAndDoRequest();
	};
	RouteService.prototype.setEndCoord = function (pos) {
		this.route.setEnd(pos);
		this.checkCoordinatesAndDoRequest();
	};
	RouteService.prototype.checkCoordinatesAndDoRequest = function () {
		if (!this.route.start || !this.route.end) {
			return;
		}
		console.debug("Would route now!");
	};

	return app.factory('routeService', ['$rootScope', function ($rootScope) {
		return new RouteService($rootScope);
	}]);
});
