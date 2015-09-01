/*
 * Copyright (c) 2015 Andreas Wolf
 *
 * See te LICENSE file in the project root for further copyright information.
 */
define(['angular-route'], function () {
	var config = function ($routeProvider) {
		$routeProvider
			.when('/home', {controller: 'routeController', templateUrl: '../tpl/map.html'})
			.otherwise({redirectTo: '/home'});
	};

	config.$inject = ['$routeProvider'];

	return config;
});
