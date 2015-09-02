/*
 * Copyright (c) 2015 Andreas Wolf
 *
 * See te LICENSE file in the project root for further copyright information.
 */
define(['jquery', 'leaflet', 'app/service/routeService', 'app/service/map', 'app/service/mapContextMenu', 'leaflet.contextmenu'], function ($, L) {
	var controller = function () {
		// currently nothing to do here, the controller just coordinates construction of the model
	};

	controller.$inject = ['mapService', 'routeService', 'mapContextMenu'];

	return controller;
});
