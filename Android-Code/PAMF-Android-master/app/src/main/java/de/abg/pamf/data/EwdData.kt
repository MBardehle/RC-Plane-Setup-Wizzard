package de.abg.pamf.data

import androidx.lifecycle.MutableLiveData
import de.abg.pamf.remote.BluetoothMessage
object EwdData {

    // Winkel_A, Winkel_B, Winkel_DIFF
    var angles = MutableLiveData<FloatArray>().apply {
        value = floatArrayOf(0f,0f,0f)
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
            "EWD#0#START",
            Pair("EWD#0#START#OK", fun(response) : Boolean  {return true}),
            Pair("EWD#A#([-]?\\d+.\\d)#B#([-]?\\d+.\\d)#AB#([-]?\\d+.\\d)", fun(response) : Boolean  {
                val match = Regex("EWD#A#([-]?\\d+.\\d)#B#([-]?\\d+.\\d)#AB#([-]?\\d+.\\d)").find(response)!!
                val (a,b,c) = match.destructured
                angles.postValue(floatArrayOf(a.toFloat(),b.toFloat(),c.toFloat()))
                return true
            }),
            timeout = 2000,
            timeoutFunction = fun() {
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
            "EWD#0#END",
            Pair("EWD#0#END#OK", fun(response) : Boolean  {return false})
        )
    }

    fun setZero(ruder : String){
        BluetoothMessage(
            true,
            true,
            "EWD#"+ruder+"#ZERO",
            Pair("EWD#"+ruder+"#ZERO#OK", fun(response) : Boolean  {
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

