'use strict';
/*
 * Copyright (c) 2015 Andreas Wolf
 *
 * See te LICENSE file in the project root for further copyright information.
 */

module.exports = function (grunt) {

	var app = '../public',
		componentsDir = app + '/components';

	grunt.loadNpmTasks('grunt-contrib-compass');
	grunt.loadNpmTasks('grunt-regarde');
	grunt.loadNpmTasks('grunt-bower-requirejs');

	grunt.initConfig({
		watch: {
			compass: {
				files: ['./styles/*.scss'],
				tasks: ['compass']
			}
		},
		compass: {
			options: {
				sassDir: './styles',
				cssDir: app + '/css',
				imagesDir: app + '/img',
				//javascriptsDir: app + /scripts',
				//fontsDir: app + /styles/fonts',
				//importPath: app + /components',
				relativeAssets: true,
				importPath: [componentsDir]
			},
			dist: {},
			server: {
				options: {
					debugInfo: true
				}
			}
		},
		bowerRequirejs: {
			target: {
				rjsConfig: app + '/js/config.js',
				'exclude-dev': true
			}
		}
	});

	grunt.renameTask('regarde', 'watch');

	grunt.registerTask('build', [
		'compass:dist',
		'bowerRequirejs'
	]);
};
