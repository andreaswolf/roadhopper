/*
 * Copyright (c) 2015 Andreas Wolf
 *
 * See te LICENSE file in the project root for further copyright information.
 */
define(['app/base', 'underscore', 'leaflet', 'URIjs', 'app/model/route'], function (app, _, L, URI, Route) {

	var $http, host;

	// TODO move this block to a general configuration part
	if (location.port === '') {
		host = location.protocol + '//' + location.hostname;
	} else {
		host = location.protocol + '//' + location.hostname + ":" + location.port;
	}
	var prepareRequestUrl = function (points) {
		var url = host + '/roadhopper/route';

		return URI(url).search({point: points, type: 'json'});
	};

	var RouteService = function ($rootScope) {
		console.debug("Creating route service");
		this.route = new Route($rootScope);
		this.$rootScope = $rootScope;
		var instance = this;
		this.$rootScope.$on('routePointsUpdated', function () {
			instance.checkCoordinatesAndDoRequest();
		});
	};
	_.extend(RouteService.prototype, {
		setStartCoord: function (pos) {
			this.route.setStart(pos);
			this.checkCoordinatesAndDoRequest();
		},
		addIntermediateCoord: function (pos) {
			this.route.addIntermediate(pos);
			this.checkCoordinatesAndDoRequest();
		},
		removeByIndex: function (index) {
			if (index == 0) {
				this.setStartCoord(null);
			} else if (index == this.route.points.length - 1) {
				this.setEndCoord(null);
			} else {
				this.route.removeIntermediate(index - 1);
			}
		},
		setEndCoord: function (pos) {
			this.route.setEnd(pos);
			this.checkCoordinatesAndDoRequest();
		},
		checkCoordinatesAndDoRequest: function () {
			if (!this.route.start || !this.route.end) {
				return;
			}
			console.debug("Would route now!");
			var instance = this;

			console.debug("points: ", instance.route.getPointCoordinates());
			$http.get(prepareRequestUrl(instance.route.getPointCoordinates())).
				then(function (json) {
					instance.$rootScope.$emit('routeFetched', json['data']);
				}, function (response) {
					// called asynchronously if an error occurs
					// or server returns response with an error status.
				});
		}
	});


	return app.factory('routeService', ['$rootScope', '$http', function ($rootScope, http) {
		$http = http;
		return new RouteService($rootScope);
	}]);
});
