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

	function getIconForTower(node) {
		if (node["info"] == "trafficLight") {
			return trafficLightIcon;
		} else {
			return towerNodeIcon;
		}
	}

	for (var i = 0; i < jsonData["towerNodes"].length; ++i) {
		node = jsonData["towerNodes"][i];
		console.debug(node);
		L.marker([node['lat'], node['lon']], {icon: getIconForTower(node)}).addTo(routingLayer);
	}

};

GHRequest.prototype.createURL = function () {
    return this.createPath(this.host + "/roadhopper/route?" + this.createPointParams(false) + "&type=" + this.dataType + "&key=" + this.key);
};
