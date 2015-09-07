/*
 * Copyright (c) 2015 Andreas Wolf
 *
 * See te LICENSE file in the project root for further copyright information.
 */

package info.andreaswolf.roadhopper.simulation

import info.andreaswolf.roadhopper.road.Route
import info.andreaswolf.roadhopper.simulation.SimulationParameters._
import info.andreaswolf.roadhopper.simulation.vehicle.VehicleParameters


object SimulationParameters {
	val velocityControllerDefaults = new VelocityControllerParameters(-0.0069, -2.59e-3, 5.35e-8)

	/**
	 * Parameters for the driver’s "brain" that controls the target velocity
	 */
	class VelocityControllerParameters(val proportionalGain: Double, val integratorGain: Double, val differentiatorGain: Double)

	/**
	 * Parameters for the driver’s input instruments
	 */
	class PedalParameters(val gasPedalGain: Double, val brakePedalGain: Double)

}

/**
 * An abstracted set of parameters for a simulation run, which can be changed by an end user.
 *
 * This is intended as a central encapsulation of all parameters that might influence the simulation within the defined
 * model, i.e. the parameters for the control loop and the vehicle (= the plant in control theory speak).
 *
 * Once such parameters have been identified, this could also include parameters that are understandable for an average
 * end user to modify parts of the simulation (e.g. the driver "aggressiveness" or safety margins like the comfortable
 * breaking acceleration). The latter of those parameters is actually already used, but not configurable for the moment,
 * as the target velocity estimator, where it is used for the look ahead distance, does not seem to work particularly
 * well.
 */
class SimulationParameters(val velocityController: VelocityControllerParameters = SimulationParameters.velocityControllerDefaults,
                           val pedal: PedalParameters, val vehicle: VehicleParameters, val route: Route) {

}
