/*
 * Copyright (c) 2015 Andreas Wolf
 *
 * See te LICENSE file in the project root for further copyright information.
 */
define([
		'app/service/routeService',
		'app/service/map',
		'app/service/mapContextMenu',
		'app/service/RoutingLayer',
		'app/service/RouteMarkers'
	],
	function () {

		var controller = function ($rootScope, mapService, routeService, mapContextMenu, RoutingLayer, RouteMarkers) {
			$rootScope.$on('routePointsUpdated', function () {
				if (routeService.route.isRoutable) {
				}
			})
		};

		controller.$inject = ['$rootScope', 'mapService', 'routeService', 'mapContextMenu', 'RoutingLayer', 'RouteMarkers'];

		return controller;
	});
