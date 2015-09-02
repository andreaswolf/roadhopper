/*
 * Copyright (c) 2015 Andreas Wolf
 *
 * See te LICENSE file in the project root for further copyright information.
 */
define(['app/base', 'app/config', 'app/routeController', 'app/controller/routePointList'],
	function (app, config, routeController, routePointList) {
		app.config(config);
		app.controller('routeController', routeController);
		app.controller('routePointList', routePointList);
	}
);
