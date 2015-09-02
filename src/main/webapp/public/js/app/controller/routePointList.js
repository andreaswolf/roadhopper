/*
 * Copyright (c) 2015 Andreas Wolf
 *
 * See te LICENSE file in the project root for further copyright information.
 */
define(['jquery', 'leaflet', 'app/service/routeService', 'leaflet.contextmenu'], function ($, L) {

	/**
	 * The controller for displaying the list of route points in the left bar.
	 *
	 * @param $scope
	 * @param $rootScope
	 * @param routeService
	 */
	var controller = function ($scope, $rootScope, routeService) {

		console.debug("creating route point list controller");
		$scope.route = routeService.route;
		$rootScope.$on('routePointsUpdated', function () {
			$scope.$apply();
		})
	};
	controller.$inject = ['$scope', '$rootScope', 'routeService'];

	return controller;
});
