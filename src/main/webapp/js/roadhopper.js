drawRouteCallback = function (jsonData) {
	var node;
	var trafficLightIcon = L.icon({
		iconUrl: './img/traffic_light.png',
		shadowSize: [50, 64],
		shadowAnchor: [4, 62],
		iconAnchor: [12, 12]
	});

	for (var i = 0; i < jsonData["towerNodes"].length; ++i) {
		node = jsonData["towerNodes"][i];
		console.debug(node);
		L.marker([node['lat'], node['lon']], {icon: trafficLightIcon}).addTo(routingLayer);
	}

};