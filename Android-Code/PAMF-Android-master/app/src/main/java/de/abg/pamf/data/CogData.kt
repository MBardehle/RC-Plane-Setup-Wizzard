package de.abg.pamf.data

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import de.abg.pamf.remote.BluetoothCommunicator
import de.abg.pamf.remote.BluetoothMessage
import kotlin.math.pow
import kotlin.math.round
import kotlin.math.roundToInt
import kotlin.math.sqrt

object CogData {

    /**
     * In dieser Klasse stehen nur Daten, die vom Nutzer eingestellt werden.
     * Die Daten, die vom Mikrocontroller gemessen werden stehen in TODO
     */


    private const val NAME = "COG_DATA"
    private const val TAG = "COG_DATA"
    private const val MODE = Context.MODE_PRIVATE
    private lateinit var preferences: SharedPreferences

    // Namen der Einstellungen und Default-Werte
    private val TYPE = Pair("TYPE", 1)
    private val DISTANCE_1_2 = Pair("DISTANCE_1_2", 0)
    private val DISTANCE_FRONT = Pair("DISTANCE_FRONT", 0)
    private val DISTANCE_TARGET = Pair("DISTANCE_TARGET", 0)
    private val HEIGHT_SCALE_1 = Pair("HEIGHT_SCALE_1", 0)

    private val SCALE_1 = Pair("SCALE_1", 1000)
    private val SCALE_2 = Pair("SCALE_2", 1000)
    private val SCALE_3 = Pair("SCALE_3", 1000)



    fun init(context : Context){
        preferences = context.getSharedPreferences(
            NAME,
            MODE
        )

        weight_sum.addSource(weight){
            weight_sum.value = it[0] + it[1] + it[2]
        }

        center_of_gravity.addSource(weight){
            if(it[0] == 0 || it[1] == 0 || it[2] == 0 || distance_1_2_real == 0f)
                center_of_gravity.value = 0
            else
                if(type == 1)               //type 2 = Bugfahrwerk Waage 1 = vorne
                    center_of_gravity.value = (((it[0] * distance_1_2_real) / (it[0] + it[1] + it[2])) - distance_front).roundToInt()
                else                        //type 1 = Heckfahrwerk Waage 1 (it[0]) = hinten
                    center_of_gravity.value = ((((it[1] + it[2]) * distance_1_2_real) / (it[0] + it[1] + it[2])) - (distance_1_2_real - distance_front)).roundToInt()
        }

        center_of_gravity_diff.addSource(
            center_of_gravity
        ){
            center_of_gravity_diff.value = it - distance_target
        }

        scale_1_livedata.postValue(scale_1)
        scale_2_livedata.postValue(scale_2)
        scale_3_livedata.postValue(scale_3)

        BluetoothCommunicator.addListener(BluetoothMessage(false, false, "",
            Pair("CG#1#(1|5|10)", fun(response) : Boolean  {
                val match = Regex("CG#1#(1|5|10)").find(response)!!
                val (value) = match.destructured
                preferences.edit().putInt(SCALE_1.first, value.toInt() * 1000).apply()
                scale_1_livedata.postValue(value.toInt() * 1000)
                return false }),
            send = false
        ))
        BluetoothCommunicator.addListener(BluetoothMessage(false, false, "",
            Pair("CG#2#(1|5|10)", fun(response) : Boolean  {
                val (value) = Regex("CG#2#(1|5|10)").find(response)!!.destructured
                preferences.edit().putInt(SCALE_2.first, value.toInt() * 1000).apply()
                scale_2_livedata.postValue(value.toInt() * 1000)
                return false }),
            send = false
        ))
        BluetoothCommunicator.addListener(BluetoothMessage(false, false, "",
            Pair("CG#3#(1|5|10)", fun(response) : Boolean  {
                val match = Regex("CG#3#(1|5|10)").find(response)!!
                val (value) = match.destructured
                preferences.edit().putInt(SCALE_2.first, value.toInt() * 1000).apply()
                scale_3_livedata.postValue(value.toInt() * 1000)
                return false }),
            send = false
        ))
    }

    var type : Int
        get() = preferences.getInt(TYPE.first, TYPE.second)
        set(value) = preferences.edit().putInt(
            TYPE.first, value).apply()

    var distance_1_2 : Int
        get() = preferences.getInt(DISTANCE_1_2.first, DISTANCE_1_2.second)
        set(value) = preferences.edit().putInt(
            DISTANCE_1_2.first, value).apply()

    var distance_front : Int
        get() = preferences.getInt(DISTANCE_FRONT.first, DISTANCE_FRONT.second)
        set(value) = preferences.edit().putInt(
            DISTANCE_FRONT.first, value).apply()

    var distance_target : Int
        get() = preferences.getInt(DISTANCE_TARGET.first, DISTANCE_TARGET.second)
        set(value) = preferences.edit().putInt(
            DISTANCE_TARGET.first, value).apply()

    var height_scale_1 : Int
        get() = preferences.getInt(HEIGHT_SCALE_1.first, HEIGHT_SCALE_1.second)
        set(value) = preferences.edit().putInt(
            HEIGHT_SCALE_1.first, value).apply()

    var distance_1_2_real : Float = 0f
        get() = sqrt(distance_1_2.toFloat().pow(2) - height_scale_1.toFloat().pow(2))

    var scale_1 : Int
        get() = preferences.getInt(SCALE_1.first, SCALE_1.second)
        set(value) {
            preferences.edit().putInt(SCALE_1.first, value).apply()
            // Änderung an Mikrocontroller senden
            sendSetScale('1', (value / 1000))
        }


    var scale_2 : Int
        get() = preferences.getInt(SCALE_2.first, SCALE_2.second)
        set(value) {
            preferences.edit().putInt(SCALE_2.first, value).apply()
            // Änderung an Mikrocontroller senden
            sendSetScale('2', (value / 1000))
        }

    var scale_3 : Int
        get() = preferences.getInt(SCALE_3.first, SCALE_3.second)
        set(value) {
            preferences.edit().putInt(SCALE_3.first, value).apply()
            // Änderung an Mikrocontroller senden
            sendSetScale('3', (value / 1000))
        }


    var scale_1_livedata = MutableLiveData<Int>()
    var scale_2_livedata = MutableLiveData<Int>()
    var scale_3_livedata = MutableLiveData<Int>()


    private fun sendSetScale(scale_no : Char, value : Int){
        BluetoothMessage(
            true,
            true,
            "CG#" + scale_no + "#SET#" + value,
            Pair("CG#" + scale_no + "#SET#" + value + "#OK", fun(_) : Boolean  {
                when (scale_no) {
                    '1' -> scale_1_livedata.postValue(value * 1000)
                    '2' -> scale_2_livedata.postValue(value * 1000)
                    '3' -> scale_3_livedata.postValue(value * 1000)
                }
                return false })
        )
    }

    fun setZero(scale_no: Char){
        // Nach einem ZERO werden Gewichte gesendet
        lateinit var message : BluetoothMessage
        message = BluetoothMessage(
            false,
            false,
            "CG#"+scale_no+"#ZERO" /*,
//            Pair("CG#"+scale_no+"#ZERO#OK", fun(_) : Boolean  {return false})
            Pair("CG#1#(-?\\d+\\.?\\d*)#2#(-?\\d+\\.?\\d*)#3#(-?\\d+\\.?\\d*)", fun(response) : Boolean  {
                val match = Regex("CG#1#(-?\\d+\\.?\\d*)#2#(-?\\d+\\.?\\d*)#3#(-?\\d+\\.?\\d*)").find(response)!!
                val (a,b,c) = match.destructured
                weight.postValue(intArrayOf(
                    round(a.toFloat()).toInt(),
                    round(b.toFloat()).toInt(),
                    round(c.toFloat()).toInt()))
                return true
                }),
            timeout = 3000,
            timeoutFunction = fun() {
                message.onDefaultTimeout()
                //Abbruch, weil zwischendurch eine andere Nachricht gesendet wurde?
                if(isRequestingWeights){
                    isRequestingWeights = false
                    requestWeights()
                }
            }*/
        )
    }


    // Ab hier sind die Daten, die über Bluetooth empfangen werden

    var isRequestingWeights = false

    fun requestWeights(){
        if(isRequestingWeights)
            return
        isRequestingWeights = true
        lateinit var message : BluetoothMessage
        message = BluetoothMessage(
            false,
            false,
            "CG#0#START",
            Pair("CG#0#Start#OK", fun(response) : Boolean  {return true}),
            Pair("CG#1#(-?\\d+\\.?\\d*)#2#(-?\\d+\\.?\\d*)#3#(-?\\d+\\.?\\d*)", fun(response) : Boolean  {
                val match = Regex("CG#1#(-?\\d+\\.?\\d*)#2#(-?\\d+\\.?\\d*)#3#(-?\\d+\\.?\\d*)").find(response)!!
                val (a,b,c) = match.destructured
                weight.postValue(intArrayOf(
                    round(a.toFloat()).toInt(),
                    round(b.toFloat()).toInt(),
                    round(c.toFloat()).toInt()))
//                Log.w(TAG, "Verarbeite Gewichte")
                return true
            }),
            timeout = 3000,
            timeoutFunction = fun() {
                Log.e(TAG, "Timeout Gewichte")
                message.onDefaultTimeout()
                //Abbruch, weil zwischendurch eine andere Nachricht gesendet wurde?
                if(isRequestingWeights){
                    isRequestingWeights = false
                    requestWeights()
                }
            }
        )
    }

    fun stopRequestingWeights(){
        isRequestingWeights = false
        BluetoothMessage(
            false,
            false,
            "CG#0#END"/*,
            Pair("CG#0#END#OK", fun(response) : Boolean  {return false})*/
        )
    }

    var weight = MutableLiveData<IntArray>().apply {
        value = intArrayOf(0,0,0)
    }
    var weight_sum = MediatorLiveData<Int>()

    var center_of_gravity = MediatorLiveData<Int>()
    var center_of_gravity_diff = MediatorLiveData<Int>()

    var scale_factor = MutableLiveData<Float>().apply {
        value = 0f
    }


    fun getStatusOfScale(caller : CogInterface, number : String){
        BluetoothMessage(
            true,
            true,
            "CG#"+number+"#STATUS",
            Pair("CG#"+number+"#([-]?\\d+\\.?\\d*)#(\\d+)#([-]?\\d+\\.?\\d*)", fun(response) : Boolean  {
//                "CG#1#-0.0#1#1997.50"
                val match = Regex("CG#"+number+"#([-]?\\d+\\.?\\d*)#(\\d+)#([-]?\\d+\\.?\\d*)").find(response)!!
                val (a,b,c) = match.destructured
                caller.onStatusRequest(number,a.toFloat(),b.toInt(),c.toFloat())
                scale_factor.postValue(c.toFloat())
                return false
            })
        )
        Log.e(TAG, "Erwarte: " + "CG#"+number+"#([-]?\\d+\\.?\\d*)#(\\d+)#([-]?\\d+\\.?\\d*)")
    }
    fun startCalibrating(caller : CogInterface, number : String){
        BluetoothMessage(
            true,
            true,
            "CG#"+number+"#READY",
            Pair("CG#"+number+"#READY#OK", fun(response) : Boolean  {
                caller.onStartCalibrating(number)
                return false
            })
        )
    }
    fun calibrate(caller : CogInterface, number : String, weight : Int){
        BluetoothMessage(
            true,
            true,
            "CG#"+number+"#CAL#"+weight,
            Pair("CG#"+number+"#CAL#OK", fun(response) : Boolean  {
                caller.onCalibrateFinished(number)
                return false
            })
        )
    }

    fun setFactor(number: String, factor: String){
        isRequestingWeights = false
        BluetoothMessage(
            true,
            true,
            "CG#"+number+"#ADDFAKTOR#"+factor,
            Pair("CG#"+number+"#ADDFAKTOR#OK", fun(response) : Boolean  {
                requestWeights()
                return false
            })
        )

    }
}

