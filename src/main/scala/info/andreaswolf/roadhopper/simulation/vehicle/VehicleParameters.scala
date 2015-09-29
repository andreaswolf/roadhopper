/*
 * Copyright (c) 2015 Andreas Wolf
 *
 * See te LICENSE file in the project root for further copyright information.
 */

package info.andreaswolf.roadhopper.simulation.vehicle

object VehicleParameters {
	/**
	 * A set of vehicle parameters for a typical compact car with an electric engine.
	 */
	val CompactCar = new VehicleParameters(
		mass = 1300,
		dragCoefficient = 0.29, dragReferenceArea = 2.4,
		wheelRadius = 32, wheelDragCoefficient = 0.012,
		maximumEnginePower = 84000, maximumEngineTorque = 200, maximumEngineRpm = 12000,
		engineEfficiencyFactor = 95,
		transmissionRatio = 10.0,
		maximumBrakingForce = 200
	)

	val Ampera = new VehicleParameters(
		mass = 1732,
		dragCoefficient = 0.27, dragReferenceArea = 2.57,
		// wheel radius rounded from 33.4 cm
		wheelRadius = 33, wheelDragCoefficient = 0.012,
		maximumEnginePower = 111000, maximumEngineTorque = 370, maximumEngineRpm = 12000,
		engineEfficiencyFactor = 95,
		transmissionRatio = 9.4,
		maximumBrakingForce = 500
	)
}

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
 * @param maximumBrakingForce The maximum braking force in Newton
 */
class VehicleParameters(val mass: Int,
                        val dragCoefficient: Double, val dragReferenceArea: Double,
                        val wheelRadius: Int, val wheelDragCoefficient: Double,
                        val maximumEnginePower: Int, val maximumEngineTorque: Int, val maximumEngineRpm: Int,
                        val engineEfficiencyFactor: Int,
                        val transmissionRatio: Double,
                        val maximumBrakingForce: Int) {
}
