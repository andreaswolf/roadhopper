drawRouteCallback = function (jsonData) {
	var node;
	var trafficLightIcon = L.icon({
		iconUrl: './img/traffic_light.png',
		iconAnchor: [12, 12]
	});
	var towerNodeIcon = L.icon({
		iconUrl: './img/tower_node.png',
		iconAnchor: [12, 12]
	});

	// remove existing information
	routingLayer.clearLayers();
	// re-add the start/end flags we removed earlier
	flagAll();

	function getIconForTower(node) {
		if (node["info"] == "trafficLight") {
			return trafficLightIcon;
		} else {
			return towerNodeIcon;
		}
	}

	L.geoJson(jsonData["points"], {
		style: function (feature) {
			return {
				color: '#'+ (function lol(m,s,c){return s[m.floor(m.random() * s.length)] +
						(c && lol(m,s,c-1));})(Math,'0123456789ABCDEF',4),
				"weight": 5,
				"opacity": 0.9
			};
		},
		onEachFeature: function (feature, layer) {
			console.debug(feature);
		}
	}).addTo(routingLayer);

	for (var i = 0; i < jsonData["towerNodes"].length; ++i) {
		node = jsonData["towerNodes"][i];
		console.debug(node);
		L.marker([node['lat'], node['lon']], {icon: getIconForTower(node)}).addTo(routingLayer);
	}

};

GHRequest.prototype.createURL = function () {
	return this.createPath(this.host + "/roadhopper/route?" + this.createPointParams(false) + "&type=" + this.dataType + "&key=" + this.key);
};