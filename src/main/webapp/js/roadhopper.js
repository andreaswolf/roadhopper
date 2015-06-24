var initMapOrig = initMap;
var roadSignLayer;
initMap = function(selectLayer) {
	initMapOrig(selectLayer);

	roadSignLayer = L.geoJson().addTo(map);
	layerControl.addOverlay(roadSignLayer, "Road Signs");
};

drawRouteCallback = function (jsonData) {
	var trafficLightIcon = L.icon({
		iconUrl: './img/traffic_light.png',
		iconAnchor: [12, 12]
	});
	var stopSignIcon = L.icon({
		iconUrl: './img/stop_sign.png',
		iconAnchor: [12, 12]
	});
	var towerNodeIcon = L.icon({
		iconUrl: './img/tower_node.png',
		iconAnchor: [12, 12]
	});

	// remove existing information
	routingLayer.clearLayers();
	roadSignLayer.clearLayers();
	// re-add the start/end flags we removed earlier
	flagAll();

	function getIconForTower(node) {
		if (node["info"] == "TrafficLight") {
			return trafficLightIcon;
		} else if (node["info"] == "StopSign") {
			return stopSignIcon;
		} else {
			return towerNodeIcon;
		}
	}

	var i = 0;
	L.geoJson(jsonData["points"], {
		filter: function(feature) {
			return feature.type == 'LineString';
		},
		style: function (feature) {
			return {
				color: '#'+ (function lol(m,s,c){return s[m.floor(m.random() * s.length)] +
						(c && lol(m,s,c-1));})(Math,'0123456789ABCDEF',4),
				"weight": 5,
				"opacity": 0.9
			};
		},
		onEachFeature: function(feature, layer) {
			if (feature.length) {
				layer.bindPopup(i + " - Länge: " + feature.length.toFixed(0) + " - "
					+ ((feature.orientation * 180/Math.PI + 360) % 360).toFixed(1) + "°"
				);
			}
			++i;
		}
	}).addTo(routingLayer);

	var t = 0;
	L.geoJson(jsonData["points"], {
		filter: function (feature) {
			return feature.type == "Point";
		},
		pointToLayer: function(feature, latlng) {
			return L.marker(latlng, {icon: getIconForTower(feature)});
		},
		onEachFeature: function(feature, layer) {
			console.debug(feature);
			if (feature.id) {
				layer.bindPopup(i + " - " + t + " - Node ID: " + feature.id);
			}
			++i; ++t;
		}
	}).addTo(roadSignLayer);

};

var originalInit = GHRequest.prototype.init;
GHRequest.prototype.init = function(params) {
	originalInit.call(this, params);
	if (params.hasOwnProperty("simplify")) {
		this.api_params["simplify"] = params["simplify"];
	}
};

GHRequest.prototype.createURL = function () {
	return this.createPath(this.host + "/roadhopper/route?" + this.createPointParams(false) + "&type=" + this.dataType + "&key=" + this.key);
};
