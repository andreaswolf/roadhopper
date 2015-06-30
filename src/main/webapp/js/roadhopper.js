var initMapOrig = initMap;
var roadSignLayer, turnLines;
initMap = function(selectLayer) {
	initMapOrig(selectLayer);

	roadSignLayer = L.geoJson().addTo(map);
	layerControl.addOverlay(roadSignLayer, "Road Signs");

	turnLines = L.geoJson().addTo(map);
	layerControl.addOverlay(turnLines, "Turn lines");
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
	var bendLeft = L.icon({
		iconUrl: './img/bend_left.png',
		iconAnchor: [12, 12]
	});
	var bendRight = L.icon({
		iconUrl: './img/bend_right.png',
		iconAnchor: [12, 12]
	});

	// remove existing information
	routingLayer.clearLayers();
	roadSignLayer.clearLayers();
	turnLines.clearLayers();
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

	function getIconForBend(node) {
		return node["direction"] == 0 ? bendLeft : bendRight;
	}

	var helpLinesStyle = {
		color: '#ff0000',
		"weight": 2,
		"opacity": 1
	};
	var lines = [];
	var j = 0, k = 0;
	console.debug(jsonData["points"].length + " route parts");
	while (j < jsonData["points"].length - 1 && k < 200) {
		++k;
		var lineA = jsonData["points"][j];

		if (lineA.type != 'LineString') {
			++j;
			continue;
		}
		// TODO we assume that the last segment will always be a LineString; this should hold, but check this again
		var lineB;
		while (!lineB && j < jsonData["points"].length - 1) {
			++j;
			if (jsonData["points"][j].type != 'LineString') {
				continue;
			}
			if (j >= jsonData["points"].length - 1) {
				break;
			}
			lineB = jsonData["points"][j];
		}
		if (!lineB) {
			// we don’t have a valid second point -> we reached the end
			break;
		}

		var orientationDiff = lineB["orientation"] - lineA["orientation"];

		if (orientationDiff > Math.PI) {
			orientationDiff -= Math.PI * 2;
		}
		orientationDiff /= 2;

		var perpendicularOrientation = lineA["orientation"] + orientationDiff;
		if (orientationDiff < 0) {
			perpendicularOrientation -= Math.PI / 2;
		} else {
			perpendicularOrientation += Math.PI / 2;
		}

		var length = Math.min(lineA["length"], lineB["length"]);
		// only draw the line if we have two short segments
		if (length < 50) {
			var intersection = lineA["coordinates"][1];
			var lineEndPoint = calculateEndPoint(intersection, length * 5, perpendicularOrientation);
			lines.push({
				coordinates: [intersection, lineEndPoint],
				type: "LineString",
				baseLength: length,
				orientation: perpendicularOrientation
			});
		}
		lineB = null;
	}
	L.geoJson(lines, {
		style: helpLinesStyle,
		onEachFeature: function(feature, layer) {
			layer.bindPopup("Base length: " + feature.baseLength.toFixed(1) + " - orientation: "
					+ toNormalizedDegrees(feature.orientation) + "°");
		}
	}).addTo(turnLines);

	var i = 0;

	/**
	 * Calculates the end point when going the given distance into the given direction from the start point.
	 *
	 * This uses plane geometry, so the earth’s rounded surface is not
	 *
	 * See http://stackoverflow.com/a/2188606 for the original inspiration.
	 *
	 * @param startPoint [double, double] startPoint The start point as [lon, lat] (sic!)
	 * @param distance double distance The distance in meters
	 * @param bearing double bearing The (initial) bearing in radians, with 0 being "north"
	 */
	function calculateEndPoint(startPoint, distance, bearing) {
		// Normalize the bearing to 0..2pi
		bearing %= Math.PI * 2;
		if (bearing < 0) {
			bearing += Math.PI * 2;
		}
		var latitude;
		if (startPoint instanceof L.LatLng) {
			latitude = startPoint.lat;
		} else {
			latitude = startPoint[1];
		}

		var dx = distance * Math.sin(bearing),
			dy = distance * Math.cos(bearing),
			deltaLon = dx / (111320 * Math.cos(latitude / 180 * Math.PI)),
			deltaLat = dy / 110540;

		if (startPoint instanceof L.LatLng) {
			return new L.LatLng(startPoint.lat + deltaLat, startPoint.lng + deltaLon);
		} else {
			return [
				startPoint[0] + deltaLon,
				startPoint[1] + deltaLat
			];
		}
	}

	/**
	 * Converts an angle in radians to 0..360°.
	 */
	function toNormalizedDegrees(orientation) {
		return ((orientation * 180 / Math.PI + 360) % 360).toFixed(1);
	}

	function toDegrees(radians) {
		return ((radians * 180 / Math.PI) % 360).toFixed(1);
	}

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
					+ toNormalizedDegrees(feature.orientation) + "°"
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
			if (feature.id) {
				layer.bindPopup(i + " - " + t + " - Node ID: " + feature.id);
			}
			++i; ++t;
		}
	}).addTo(roadSignLayer);

	L.geoJson(jsonData["additionalInfo"], {
		filter: function (feature) {
			console.debug(feature);
			return feature.type == "Point" && feature.info == "RoadBend";
		},
		pointToLayer: function(feature, latlng) {
			var orientation = feature["initialOrientation"];
			orientation += Math.PI / 2 * (feature.direction == 0 ? -1 : 1);
			var latlng2 = calculateEndPoint(latlng, 10, orientation);
			return L.marker(latlng2, {icon: getIconForBend(feature)});
		},
		onEachFeature: function(feature, layer) {
			layer.bindPopup("<strong>Road bend</strong><br />"
					+ "Richtung: " + (feature.direction == 0 ? 'links' : 'rechts') + "<br/>"
					+ "Länge: " + feature.length.toFixed(1) + "m<br />"
					+ "Winkeländerung: " + toDegrees(feature.angle) + "°<br />"
					+ "Radius: " + feature.radius.toFixed(1) + "m");
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
