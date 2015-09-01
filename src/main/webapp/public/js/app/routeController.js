/*
 * Copyright (c) 2015 Andreas Wolf
 *
 * See te LICENSE file in the project root for further copyright information.
 */
define(['jquery', 'leaflet'], function ($, L) {
	var map = null;

	var controller = function ($timeout) {
		console.debug("creating controller");

		// TODO move this configuration away

		var osmAttr = '&copy; <a href="http://www.openstreetmap.org/copyright" target="_blank">OpenStreetMap</a> contributors';

		var omniscale = L.tileLayer.wms('https://maps.omniscale.net/v1/mapsgraph-bf48cc0b/tile', {
			layers: 'osm',
			attribution: osmAttr + ', &copy; <a href="http://maps.omniscale.com/">Omniscale</a>'
		});

		var mapquest = L.tileLayer('http://{s}.mqcdn.com/tiles/1.0.0/osm/{z}/{x}/{y}.png', {
			attribution: osmAttr + ', <a href="http://open.mapquest.co.uk" target="_blank">MapQuest</a>',
			subdomains: ['otile1', 'otile2', 'otile3', 'otile4']
		});

		var mapquestAerial = L.tileLayer('http://{s}.mqcdn.com/tiles/1.0.0/sat/{z}/{x}/{y}.png', {
			attribution: osmAttr + ', <a href="http://open.mapquest.co.uk" target="_blank">MapQuest</a>',
			subdomains: ['otile1', 'otile2', 'otile3', 'otile4']
		});


		var osm = L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
			attribution: osmAttr
		});

		var osmde = L.tileLayer('http://{s}.tile.openstreetmap.de/tiles/osmde/{z}/{x}/{y}.png', {
			attribution: osmAttr,
			subdomains: ['a', 'b', 'c']
		});

		var baseMaps = {
			"MapQuest": mapquest,
			"MapQuest Aerial": mapquestAerial,
			"OpenStreetMap": osm,
			"OpenStreetMap.de": osmde
		};

		// TODO store the selected layer somewhere, have a fallback in case none is selected
		var defaultLayer = baseMaps["omniscale"];
		if (!defaultLayer)
			defaultLayer = mapquest;

		$timeout(function () {
			if (map || $('#map').length == 0) {
				return;
			}
			// default
			map = L.map('map').setView([15.0, 0.0], 2);

			omniscale.addTo(map);
		});
	};

	controller.$inject = ['$timeout'];

	return controller;
});
