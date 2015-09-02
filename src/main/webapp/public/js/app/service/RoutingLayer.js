/*
 * Copyright (c) 2015 Andreas Wolf
 *
 * See te LICENSE file in the project root for further copyright information.
 */
define(['app/base', 'jquery', 'leaflet', 'underscore', 'app/service/map', 'app/service/routeService'],
	function (app, $, L, _) {

		/**
		 * The layer used for drawing routes (calculated by GraphHopper) on the map.
		 *
		 * @param $rootScope
		 * @param mapService
		 * @param routeService
		 * @constructor
		 */
		var RoutingLayer = function ($rootScope, mapService, routeService) {
			console.debug("Constructing routing layer", mapService);

			var instance = this;
			console.debug(this);
			$rootScope.$on('routePointsUpdated', function () {
				instance.redraw();
				$rootScope.$broadcast('RoutingLayer::redraw');
			});

			this.layer = L.geoJson().addTo(mapService.map);
			this.layer.options = {
				style: {color: "#00cc33", "weight": 5, "opacity": 0.6}, // route color and style
				contextmenu: true,
				contextmenuItems: [{
					text: 'Route ',
					disabled: true,
					index: 0,
					state: 3
				}, {
					text: 'Set intermediate',
					callback: $.proxy(routeService.addIntermediateCoord, routeService),
					index: 1,
					state: 3
				}, {
					separator: true,
					index: 2,
					state: 3
				}],
				contextmenuAtiveState: 3
			};

			this.routeService = routeService;
		};

		_.extend(RoutingLayer.prototype, {
			redraw: function() {
				this.layer.clearLayers();
			}
		});

		return app.service('RoutingLayer', ['$rootScope', 'mapService', 'routeService', RoutingLayer]);

	});
