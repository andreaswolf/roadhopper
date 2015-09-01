/*
 * Copyright (c) 2015 Andreas Wolf
 *
 * See te LICENSE file in the project root for further copyright information.
 */
define(['angular', 'app/config', 'app/routeController'],
	function (angular, config, routeController) {
		var app = angular.module('roadhopperApp', ['ngRoute']);//, [, 'ngResource', 'ngGrid']);
		app.config(config);
		app.controller('routeController', routeController);
	}
);
