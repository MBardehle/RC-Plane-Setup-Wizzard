package de.abg.pamf.remote

import android.os.Handler
import android.util.Log
import androidx.core.os.postDelayed


class BluetoothMessage (val blocking : Boolean = true, val resendOnError : Boolean = true, val message : String, vararg response : Pair<String, ((String)->Boolean)>, val timeout : Long = 5000, timeoutFunction : (()-> Unit)? = null, val send :Boolean = true) {

    /*
     * Es werden mögliche Antworten auf die Nachricht registriert.
     * Zu jeder dieser Antworten wird eine Funktion mitgegeben, die aufgerufen wird, falls die Antwort empfangen wird.
     * Diese Funktion antwortet true, wenn weitere Nachrichten erwartet werden. Dabei wird bei jeder emofangenen Nachricht der Timeout zurückgesetzt
     * Wenn sie false antwortet, wir der Listener entfernt.
     */
    val response = response

    private val timeoutFunction = timeoutFunction ?: (fun() { onDefaultTimeout()})
    private val timeoutRunnable = Runnable {if(responded == false) timeoutFunction()}
    private var sent = false
    private var responded = false

    var handler : Handler

    companion object {
        val TAG = "BTMessage"
    }

    init {
        handler = Handler()
        if(send){
            // Bei der Initialisierung an den Bluetooth Communicator senden
            BluetoothCommunicator.sendMessage(this)
        }
    }

    fun isSent() {
        // Die Nachricht wurde vom BluetoothCommunicator gesendet
        sent = true

        // Wenn es keine Funktionen gibt, die auf Antwort warten, wird auch kein Timeout überprüft
        if(response.size > 0) {
            handler.postDelayed(timeoutRunnable, timeout)
        }
    }

    fun onConnectionRestart(){
        if(resendOnError){
            BluetoothCommunicator.sendMessage(this)
        }
    }

    fun onDefaultTimeout(){
        Log.e(TAG, "TIMEOUT (after " + timeout + "ms): " + message)
        if(resendOnError){
            BluetoothCommunicator.sendMessage(this)
        } else {
            // Diesen Response Listener entfernen
            BluetoothCommunicator.unregisterMessageListener(this)
        }
    }

    fun onResponse(responseText : String, responseFunction : ((String)->Boolean)){
        responded = true
        // Timeout stoppen
        handler.removeCallbacks(timeoutRunnable)
        if(responseFunction(responseText)){
            // wir erwarten eine weitere Antwort
            responded = false
            // Timeout neu starten
            handler.postDelayed(timeoutRunnable, timeout)
        } else {
            // Diesen Response Listener entfernen
            BluetoothCommunicator.unregisterMessageListener(this)
        }
    }


}