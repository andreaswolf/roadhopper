require.config({
	shim: {
		angular: {
			exports: 'angular'
		},
		'angular-route': {
			deps: ['angular']
		},
		leaflet: {
			exports: 'L'
		}
	},
	paths: {
		'foundation-apps': '../components/foundation-apps/dist/js/foundation-apps',
		jquery: '../components/jquery/dist/jquery',
		leaflet: '../components/leaflet/dist/leaflet-src',
		'leaflet.contextmenu': '../components/leaflet.contextmenu/dist/leaflet.contextmenu',
		'leaflet.contextmenu-src': '../components/leaflet.contextmenu/dist/leaflet.contextmenu-src',
		requirejs: '../components/requirejs/require',
		angular: '../components/angular/angular',
		'angular-route': '../components/angular-route/angular-route'
	},
	packages: [

	]
});
