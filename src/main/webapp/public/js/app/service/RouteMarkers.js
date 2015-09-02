/*
 * Copyright (c) 2015 Andreas Wolf
 *
 * See te LICENSE file in the project root for further copyright information.
 */

define(['app/base', 'underscore', 'leaflet', 'app/service/routeService', 'app/service/RoutingLayer'], function (app, _, L) {

	var RouteMarkers = function ($rootScope, routeService, RoutingLayer) {
		var instance = this;
		console.debug($rootScope);

		this.markers = {};
		this.routingLayer = RoutingLayer;
		this.routeService = routeService;
		$rootScope.$on('RoutingLayer::redraw', function () {
			instance.update(instance.routeService.route.points);
		})
	};
	_.extend(RouteMarkers.prototype, {
		update: function (points) {

			for (var i = 0; i < points.length; ++i) {
				if (points[i]) {
					var role = i == 0 ? 'start' : (i == points.length - 1 ? 'end' : 'intermediate' + i);

					this._updateMarker(i, role, points[i].latlng);
				}
			}
		},
		_updateMarker: function (index, role, coord) {
			var instance = this;
			var marker = L.marker([coord.lat, coord.lng], {
				icon: ((role === 'start') ? iconFrom : ((role === 'end') ? iconTo : iconInt)),
				draggable: true,
				contextmenu: true,
				contextmenuItems: [{
					text: 'Marker ' + ((role === 'start') ?
						'Start' : ((role === 'end') ? 'End' : 'Intermediate ' + index)),
					disabled: true,
					index: 0,
					state: 2
				}, {
					text: 'Set as ' + ((role !== 'end') ? 'End' : 'Start'),
					// TODO reimplement:
					// callback: (role !== 'end') ? setToEnd : setToStart,
					index: 2,
					state: 2
				}, {
					text: 'Delete from Route',
					// TODO reimplement:
					callback: function () {
						instance.routeService.removeByIndex(index);
					},
					index: 3,
					state: 2,
					disabled: false//(toFrom !== -1 && ghRequest.route.size() === 2) ? true : false // prevent to and from
				}, {
					separator: true,
					index: 4,
					state: 2
				}],
				contextmenuAtiveState: 2
			}).addTo(this.routingLayer.layer);

			var instance = this;
			marker.on('dragend', function (e) {
				console.debug("dragged point to ", e);
				var coordinates = e.target.getLatLng();
				instance.routeService.route.movePoint(index, coordinates);
			});
		}
	});

	var iconFrom = L.icon({
		iconUrl: './img/marker-icon-green.png',
		shadowSize: [50, 64],
		shadowAnchor: [4, 62],
		iconAnchor: [12, 40]
	});

	var iconTo = L.icon({
		iconUrl: './img/marker-icon-red.png',
		shadowSize: [50, 64],
		shadowAnchor: [4, 62],
		iconAnchor: [12, 40]
	});

	var iconInt = L.icon({
		iconUrl: './img/marker-icon-blue.png',
		shadowSize: [50, 64],
		shadowAnchor: [4, 62],
		iconAnchor: [12, 40]
	});

	app.service('RouteMarkers', ['$rootScope', 'routeService', 'RoutingLayer', RouteMarkers]);
});
