/*
 * Copyright (c) 2015 Andreas Wolf
 *
 * See te LICENSE file in the project root for further copyright information.
 */
define(['app/base', 'leaflet'], function (app, L) {
	var osmAttr = '&copy; <a href="http://www.openstreetmap.org/copyright" target="_blank">OpenStreetMap</a> contributors';

	var layers = {
		"OmniScale": L.tileLayer.wms('https://maps.omniscale.net/v1/mapsgraph-bf48cc0b/tile', {
			layers: 'osm',
			attribution: osmAttr + ', &copy; <a href="http://maps.omniscale.com/">Omniscale</a>'
		}),
		"MapQuest": L.tileLayer('http://{s}.mqcdn.com/tiles/1.0.0/osm/{z}/{x}/{y}.png', {
			attribution: osmAttr + ', <a href="http://open.mapquest.co.uk" target="_blank">MapQuest</a>',
			subdomains: ['otile1', 'otile2', 'otile3', 'otile4']
		}),
		"MapQuest Aerial": L.tileLayer('http://{s}.mqcdn.com/tiles/1.0.0/sat/{z}/{x}/{y}.png', {
			attribution: osmAttr + ', <a href="http://open.mapquest.co.uk" target="_blank">MapQuest</a>',
			subdomains: ['otile1', 'otile2', 'otile3', 'otile4']
		}),
		"OpenStreetMap": L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
			attribution: osmAttr
		}),
		"OpenStreetMap.de": L.tileLayer('http://{s}.tile.openstreetmap.de/tiles/osmde/{z}/{x}/{y}.png', {
			attribution: osmAttr,
			subdomains: ['a', 'b', 'c']
		})
	};

	// TODO store the selected layer somewhere, have a fallback in case none is selected
	var defaultLayer = layers["OmniScale"];

	var selectedPosition = [15.0, 0.0];
	var selectedZoomLevel = 2;

	var MapService = function () {
		console.debug("creating map service");

		var instance = this;
		this.map = L.map('map', {
			layers: [defaultLayer],
			contextmenu: true,
			contextmenuWidth: 145,
			contextmenuItems: [{
				separator: true,
				index: 3,
				state: ['set_default']
			}, {
				text: 'Show coordinates',
				callback: function (e) {
					alert(e.latlng.lat + "," + e.latlng.lng);
				},
				index: 4,
				state: [1, 2, 3]
			}, {
				text: 'Center map here',
				callback: function (e) {
					instance.map.panTo(e.latlng);
				},
				index: 5,
				state: [1, 2, 3]
			}]
		}).setView(selectedPosition, selectedZoomLevel);
	};

	return app.factory('mapService', function() {
		return new MapService();
	})
});
