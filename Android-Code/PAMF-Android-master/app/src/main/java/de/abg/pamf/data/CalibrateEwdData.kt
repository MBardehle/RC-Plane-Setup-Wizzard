package de.abg.pamf.data

import de.abg.pamf.remote.BluetoothMessage
import de.abg.pamf.ui.calibrate.CalibrateTiltSensorFragment

object CalibrateEwdData {

    fun start(caller: CalibrateTiltSensorFragment, sensor : Char){
        BluetoothMessage(
            true,
            true,
            "EWD#"+sensor+"#CAL",
            Pair("EWD#"+sensor+"#CAL#READY", fun(response) : Boolean  {
                nextStep(caller, sensor)
                return false
            })
        )
    }

    fun nextStep(caller: CalibrateTiltSensorFragment, sensor: Char){
        BluetoothMessage(
            true,
            false,
            "EWD#"+sensor+"#CAL#STEP",
            Pair("EWD#"+sensor+"#CAL#STEP#(\\d)", fun(response) : Boolean  {
                val match = Regex("EWD#"+sensor+"#CAL#STEP#(\\d)").find(response)!!
                val (a) = match.destructured
                if(a.toInt() == 7){
                    return true //CAL STEP 7 wird nicht verarbeitet, da direkt folgend CAL#OK kommt
                }
                caller.onNextStep(a.toInt())
                return false
            }),
            Pair("EWD#"+sensor+"#CAL#OK", fun(response) : Boolean  {
                caller.onCalibrationFinished()
                return false
            })
        )
    }
}