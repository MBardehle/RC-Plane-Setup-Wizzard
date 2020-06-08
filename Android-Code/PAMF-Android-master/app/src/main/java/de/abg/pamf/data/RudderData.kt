package de.abg.pamf.data

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import de.abg.pamf.remote.BluetoothMessage
import kotlin.math.*

object RudderData {


    private const val NAME = "ANGLE_DATA"
    private const val TAG = "ANGLE_DATA"
    private const val MODE = Context.MODE_PRIVATE
    private lateinit var preferences: SharedPreferences

    // Namen der Einstellungen und Default-Werte
    private val MODE_REVERSE = Pair("MODE_REVERSE", false)
    private val MAX_DIFF = Pair("MAX_DIFF", 1.5f)
    private val RUDDER_WIDTH = Pair("DISTANCE_1_2", 80)


    // Winkel_A, Winkel_B, Winkel_DIFF
    var angles = MutableLiveData<FloatArray>().apply {
        value = floatArrayOf(0f,0f,0f)
    }
    // Ruderweg_A, Ruderweg_B, Ruderweg_DIFF
    var rudder_ways = MediatorLiveData<IntArray>()


    // Gleichlauf = false; Gegenlauf = true
    var mode_reverse : Boolean
        get() = preferences.getBoolean(MODE_REVERSE.first, MODE_REVERSE.second)
        set(value) {
            preferences.edit().putBoolean(MODE_REVERSE.first, value).apply()
            BluetoothMessage(
                true,
                true,
                "RUD#B#REVERSE#" + if(value) "1" else "0",
                Pair("RUD#B#REVERSE#" + (if(value) "1" else "0") + "#OK", fun(response) : Boolean  {
                    Log.e(TAG, "RUD Modus Reverse: " + response)
                    // Datenabfrage neu starten
                    if(isRequestingData){
                        isRequestingData = false
                        requestData()
                    }
                    return false
                })
            )
        }

    var max_diff : Float
        get() = preferences.getFloat(MAX_DIFF.first, MAX_DIFF.second)
        set(value) = preferences.edit().putFloat(MAX_DIFF.first, value).apply()

    var rudder_width : Int
        get() = preferences.getInt(RUDDER_WIDTH.first, RUDDER_WIDTH.second)
        set(value) = preferences.edit().putInt(RUDDER_WIDTH.first, value).apply()


    fun init(context : Context){
        preferences = context.getSharedPreferences(NAME, MODE)

        rudder_ways.addSource(angles){
            val rad_a = it[0] * PI / 180
            val rad_b = it[1] * PI / 180
            val rad_d = it[2] * PI / 180

            // Sinus
            val a  = (sin(rad_a) * rudder_width).roundToInt()
            val b  = (sin(rad_b) * rudder_width).roundToInt()
            val diff  = a - b

            // Abstand Spitze vorher zu nachher
/*            val a  = (sqrt( (1- cos(rad_a)).pow(2) + sin(rad_a).pow(2)) * rudder_width).roundToInt()
            val b  = (sqrt( (1- cos(rad_b)).pow(2) + sin(rad_b).pow(2)) * rudder_width).roundToInt()
            val diff  = (sqrt( (1- cos(rad_d)).pow(2) + sin(rad_d).pow(2)) * rudder_width).roundToInt()*/
            rudder_ways.value = intArrayOf(a,b,diff)

        }
    }


    var isRequestingData = false
    fun requestData(){
//        TODO: Wenn die Sensoren nicht kalibriert sind, kann man "EWD#A#-45.0#B#nan#AB#nan;" oder ähnliches erhalten
        if(isRequestingData)
            return
        isRequestingData = true
        lateinit var message : BluetoothMessage
        message = BluetoothMessage(
            false,
            false,
            "RUD#0#START",
            Pair("RUD#0#START#OK", fun(response) : Boolean  {
//                Log.d(TAG, "Beginne mit der Messung der Winkel")
                return true
            }),
            Pair("RUD#A#([-]?\\d+.\\d)#B#([-]?\\d+.\\d)#AB#([-]?\\d+.\\d)", fun(response) : Boolean  {
                val match = Regex("RUD#A#([-]?\\d+.\\d)#B#([-]?\\d+.\\d)#AB#([-]?\\d+.\\d)").find(response)!!
                val (a,b,c) = match.destructured
                angles.postValue(floatArrayOf(a.toFloat(),b.toFloat(),c.toFloat()))
                return true
            }),
            timeout = 10000,
            timeoutFunction = fun() {
                Log.e(TAG, "Tmeout Ruder")
                message.onDefaultTimeout()
                //Abbruch, weil zwischendurch eine andere Nachricht gesendet wurde?
                if(isRequestingData){
                    isRequestingData = false
                    requestData()
                }
            }
        )
    }
    fun stopRequestingData(){
        isRequestingData = false
        BluetoothMessage(
            false,
            false,
            "RUD#0#END",
            Pair("RUD#0#END#OK", fun(response) : Boolean  {return false})
        )
    }

    fun setZero(ruder : String){
        BluetoothMessage(
            true,
            false,
            "RUD#"+ruder+"#ZERO",
            Pair("RUD#"+ruder+"#ZERO#OK", fun(response) : Boolean  {
                Log.e(TAG, "RUD Antwort TARA ")
                // Datenabfrage neu starten
                if(isRequestingData){
                    isRequestingData = false
                    requestData()
                }
                return false
            })
        )
    }

}

