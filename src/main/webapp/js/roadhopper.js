var initMapOrig = initMap;
var roadSignLayer, turnLines;

/**
 * Converts an angle in radians to 0..360°.
 */
function toNormalizedDegrees(orientation) {
	return ((orientation * 180 / Math.PI + 360) % 360).toFixed(1);
}

function toDegrees(radians) {
	return ((radians * 180 / Math.PI) % 360).toFixed(1);
}

var graphHopperIntegration = {
	drawRouteCallbacks: [],
	modulesSelector: '#modules',

	/**
	 * The LeafletJS map. Will be set when the map is initialized.
	 */
	map: null,

	initMapCallbacks: [],

	drawRoute: function(jsonData) {
		for (var i = 0; i < this.drawRouteCallbacks.length; ++i) {
			var callback = this.drawRouteCallbacks[i];
			var context = typeof(callback[1]) == "object" ? callback[1] : this;
			callback[0].call(context, jsonData);
		}
	},

	addRouteDrawCallback: function(callback, context) {
		this.drawRouteCallbacks.push([callback, context]);
	},

	addModule: function(name, label, contents) {
		var module = $('<section class="module" data-module="' + name + '" />');

		module.append($('<div class="header">').text(label));
		var $content = $('<div class="content">');
		$content.append(contents);
		$content.hide();
		module.append($content);

		var that = this;
		$(function() {
			$(that.modulesSelector).append(module);
		})
	}
};
drawRouteCallback = function(jsonData) {
	graphHopperIntegration.drawRoute(jsonData);
};
initMap = function(selectLayer) {
	initMapOrig(selectLayer);

	roadSignLayer = L.geoJson().addTo(map);
	layerControl.addOverlay(roadSignLayer, "Road Signs");

	turnLines = L.geoJson().addTo(map);
	layerControl.addOverlay(turnLines, "Turn lines");

	// we can only initialize the map now, as it was not ready before
	graphHopperIntegration.map = map;

	var callbacks = graphHopperIntegration.initMapCallbacks;
	for (var i = 0; i < callbacks.length; ++i) {
		callbacks[i]();
	}
};

/**
 * The actual Roadhopper object
 */
var roadhopper = {
	/**
	 * Icons to use on the map
	 */
	icons: {
		trafficLight: L.icon({
			iconUrl: './img/traffic_light.png',
			iconAnchor: [12, 12]
		}),
		stopSign: L.icon({
			iconUrl: './img/stop_sign.png',
			iconAnchor: [12, 12]
		}),
		towerNode: L.icon({
			iconUrl: './img/tower_node.png',
			iconAnchor: [12, 12]
		})
	},

	drawRoute: function(jsonData) {
		routingLayer.clearLayers();
		// re-add the start/end flags we removed earlier
		flagAll();

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
						+ toNormalizedDegrees(feature.orientation) + "°"
					);
				}
				++i;
			}
		}).addTo(routingLayer);
	},

	drawRoadSigns: function(jsonData) {
		var t = 0;
		var that = this;
		L.geoJson(jsonData["points"], {
			filter: function (feature) {
				return feature.type == "Point";
			},
			pointToLayer: function(feature, latlng) {
				function lcfirst(string) {
					return string.charAt(0).toLowerCase() + string.slice(1);
				}
				return L.marker(latlng, {icon: that.icons[lcfirst(feature.info)]});
			},
			onEachFeature: function(feature, layer) {
				if (feature.id) {
					layer.bindPopup(i + " - " + t + " - Node ID: " + feature.id);
				}
				++i; ++t;
			}
		}).addTo(roadSignLayer);
	}
};

graphHopperIntegration.addRouteDrawCallback(roadhopper.drawRoute, roadhopper);
graphHopperIntegration.addRouteDrawCallback(roadhopper.drawRoadSigns, roadhopper);


legacyDrawRoute = function (jsonData) {
	var bendLeft = L.icon({
		iconUrl: './img/bend_left.png',
		iconAnchor: [12, 12]
	});
	var bendRight = L.icon({
		iconUrl: './img/bend_right.png',
		iconAnchor: [12, 12]
	});

	// remove existing information – the road sign layer should already have been cleared
	turnLines.clearLayers();

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

	L.geoJson(jsonData["additionalInfo"], {
		filter: function (feature) {
			console.debug(feature);
			return feature.type == "Point" && feature.info == "RoadBend";
		},
		pointToLayer: function(feature, latlng) {
			var orientation = feature["initialOrientation"];
			orientation += Math.PI;
			var iconPosition = calculateEndPoint(latlng, 5, orientation);
			return L.marker(iconPosition, {icon: getIconForBend(feature)});
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
graphHopperIntegration.addRouteDrawCallback(legacyDrawRoute);

var originalInit = GHRequest.prototype.init;
GHRequest.prototype.init = function(params) {
	originalInit.call(this, params);
	if (params.hasOwnProperty("simplify")) {
		this.api_params["simplify"] = params["simplify"];
	}
};

GHRequest.prototype.createURL = function () {
	return this.createPath(this.host + "/roadhopper/simulate?" + this.createPointParams(false) + "&type=" + this.dataType + "&key=" + this.key);
};

/**
 * A time series, consisting of timestamps and a position for that time.
 *
 * @param data
 * @constructor
 */
TimeSeriesDataSet = function(data) {
	this.timestamps = [];
	this.coordinates = [];
	this.data = data;

	// Extract all timestamps and coordinates from the data
	for (var time in data) {
		if (data.hasOwnProperty(time)) {
			// if any data point is undefined, the control apparently enters an uncontrolled endless loop
			if (data[time]["position"]["lon"] == undefined) {
				continue;
			}
			this.timestamps.push(parseInt(time));
			this.coordinates.push([data[time]["position"]["lon"], data[time]["position"]["lat"]]);
		}
	}
};

TimeSeriesDataSet.prototype.asGeoJSON = function() {
	return {
		"type": "Feature",
		"geometry": {
			"type": "MultiPoint",
			"coordinates": this.coordinates
		},
		"properties": {
			"time": this.timestamps
		},
		// The data as received from the server; used for processing further information
		data: this.data
	}
};

TimeSeriesDataSet.prototype.hasTime = function(time) {
	return this.timestamps.indexOf(time) > -1;
};

TimeSeriesDataSet.prototype.directionForTime = function(time) {
	return this.data[time.toString()]["direction"];
};

TimeSeriesDataSet.prototype.positionForTime = function(time) {
	return this.data[time.toString()]["position"];
};


TimeSeriesPlayback = function() {
	this.timeSeries = null;
	this.playback = null;
	this.markerDrawn = false;
};

TimeSeriesPlayback.prototype.setData = function(timeSeries) {
	if (!this.markerDrawn) {
		this.draw();
	}
	this.playback.clearData();
	this.timeSeries = timeSeries;
	this.playback.setData(timeSeries.asGeoJSON());
};

TimeSeriesPlayback.prototype.draw = function() {
	if (this.markerDrawn) {
		return;
	}
	var that = this;

	var markerIcon = L.icon({
		iconUrl: './img/marker-36.png',
		iconAnchor: [18, 18],
		iconSize: [36, 36]
	});

	this.playback = new L.Playback(map, this.data, null, {
		marker: function () {
			return {
				icon: markerIcon,
				iconAngle: 90
			}
		}
	});
	this.playback.addCallback(function(timestamp) {
		if (!that.timeSeries.hasTime(timestamp)) {
			return;
		}
		var angle = that.timeSeries.directionForTime(timestamp) * 180 / Math.PI;
		console.debug("Drawing " + timestamp + ": " + angle);

		// ATTENTION: This is an undocumented and unsupported hack to get the marker as it is not exposed via
		// the official API.
		that.playback._trackController._tracks[0]._marker.setIconAngle(angle);
	});
	this.markerDrawn = true;

	// Initialize custom control
	this.positionMarker = new L.Playback.Control(this.playback);
	this.positionMarker.addTo(map);

};


/**
 * The modules menu
 */
(function($) {
	$(function() {
		var $modules = $('#modules');

		$modules.find('.content').hide();

		$modules.on('click', '.header', function() {
			$modules.find('.content').hide();
			$(this).siblings('.content').show();
		});
		$modules.find('.header').first().trigger('click');
	});

})(jQuery);
