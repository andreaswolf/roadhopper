/*
 * Copyright (c) 2015 Andreas Wolf
 *
 * See te LICENSE file in the project root for further copyright information.
 */

define(['app/base', 'jquery', 'leaflet.contextmenu', 'app/service/map', 'app/service/routeService'], function (app, $) {

	var MapContextMenu = function($rootScope, mapService, routeService) {
		var map = mapService.map;

		var _startItem = {
			text: 'Set as start',
			callback: $.proxy(routeService.setStartCoord, routeService),
			disabled: false,
			index: 0
		};
		var _intItem = {
			text: 'Set intermediate',
			callback: $.proxy(routeService.addIntermediateCoord, routeService),
			disabled: true,
			index: 1
		};
		var _endItem = {
			text: 'Set as end',
			callback: $.proxy(routeService.setEndCoord, routeService),
			disabled: false,
			index: 2
		};
		menuStart = map.contextmenu.insertItem(_startItem, _startItem.index);
		menuIntermediate = map.contextmenu.insertItem(_intItem, _intItem.index);
		menuEnd = map.contextmenu.insertItem(_endItem, _endItem.index);

		console.debug("Constructing map context menu");

		$rootScope.$on('routePointsUpdated', function () {
			map.contextmenu.setDisabled(menuIntermediate, !(routeService.route.isRoutable()));
		});
	};

	return app.service('mapContextMenu', ['$rootScope', 'mapService', 'routeService', MapContextMenu]);
});
