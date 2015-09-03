/*
 * Copyright (c) 2015 Andreas Wolf
 *
 * See te LICENSE file in the project root for further copyright information.
 */

package info.andreaswolf.roadhopper.measurements

import java.io.File

import com.google.inject.{Guice, Inject}
import com.graphhopper.util.CmdArgs
import info.andreaswolf.roadhopper.server.RoadHopperModule
import org.slf4j.LoggerFactory

import scala.io.Source


object Importer extends App {

	val log = LoggerFactory.getLogger(this.getClass)
	val arguments = CmdArgs.read(args)

	val injector = Guice.createInjector(new RoadHopperModule(arguments))

	var measurementRepository: MeasurementRepository = injector.getInstance(classOf[MeasurementRepository])

	val basePath = arguments.get("measurements.path", "./measurements/")

	val folder = new File(basePath)

	folder.listFiles().filter(_.getName.endsWith(".txt")).sorted.foreach(importFile)

	log.info("Finished import")

	System.exit(0)


	def importFile(file: File): Unit = {
		log.info(s"Beginning to import $file")

		val items: List[MeasurementGroup] = measurementRepository.findGroupByName(file.getName)
		if (items.nonEmpty) {
			log.info(s"Skipping file because it is already present; ${items.apply(0).measurements.length} measurements")
			return
		}
		val measurementFile = new MeasurementFile(file.getName, Source.fromFile(file).getLines())

		measurementFile.measurements.foreach(measurementRepository.add)

		measurementRepository.add(new MeasurementGroup(file.getName, measurementFile.measurements.map(_.name)))
	}
}
