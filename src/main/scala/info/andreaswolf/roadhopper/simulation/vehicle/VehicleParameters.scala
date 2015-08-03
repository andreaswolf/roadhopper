/*
 * Copyright (c) 2015 Andreas Wolf
 *
 * See te LICENSE file in the project root for further copyright information.
 */

package info.andreaswolf.roadhopper.simulation.vehicle

/**
 * A set of parameters that describe a vehicle
 *
 * @param mass The vehicle’s mass in kilogram.
 * @param dragCoefficient The vehicle’s drag coefficient (c_w). Dimensionless number.
 * @param dragReferenceArea The reference area for the drag coefficient in square meters.
 * @param wheelRadius The wheel radius in centimeters
 * @param wheelDragCoefficient The wheel’s drag coefficient
 * @param maximumEnginePower The maximum engine power in Watt
 * @param maximumEngineTorque The maximum torque in Nm
 * @param maximumEngineRpm The maximum rotational speed of the engine (rotations per minute)
 * @param engineEfficiencyFactor The efficiency in percent
 * @param transmissionRatio The transmission ratio from the engine to the axle
 */
class VehicleParameters(val mass: Int,
                        val dragCoefficient: Double, val dragReferenceArea: Double,
                        val wheelRadius: Int, val wheelDragCoefficient: Double,
                        val maximumEnginePower: Int, val maximumEngineTorque: Int, val maximumEngineRpm: Int,
                        val engineEfficiencyFactor: Int,
                        val transmissionRatio: Double) {
}
