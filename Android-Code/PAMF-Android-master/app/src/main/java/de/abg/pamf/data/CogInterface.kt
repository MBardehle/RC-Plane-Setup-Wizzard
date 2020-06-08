package de.abg.pamf.data

interface CogInterface {
    fun onStatusRequest(scale: String, weight : Float, maxWeight : Int, cal_factor : Float)
    fun onStartCalibrating(scale: String)
    fun onCalibrateFinished(scale: String)
}