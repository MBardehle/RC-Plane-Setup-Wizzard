package de.abg.pamf.remote

import android.Manifest
import android.Manifest.permission.ACCESS_COARSE_LOCATION
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Looper
import android.util.Log
import android.widget.Toast
import androidx.core.app.ActivityCompat
import de.abg.pamf.MainActivity
import de.abg.pamf.data.CogData
import de.abg.pamf.data.EwdData
import de.abg.pamf.data.RudderData
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.util.*
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.regex.Pattern


object BluetoothCommunicator {

    const val REQUEST_ENABLE_BT : Int = 1
    const val REQUEST_COARSE_LOCATION_PERMISSIONS : Int = 2
    const val NAME = "PAMF_APP"
    const val MICROCONTROLLER_BT_NAME = "Air"
    var MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805f9b34fb") //.randomUUID()

    const val TAG = "PAMF BluetoothConnectio"


    lateinit var m_activity : MainActivity
    lateinit var m_bluetoothAdapter: BluetoothAdapter

    private val m_sendQueue = ConcurrentLinkedQueue<BluetoothMessage>()
    private val m_sentList = LinkedList<BluetoothMessage>()
    private var m_isConnected = false
    private var m_isRestart = false
    private var blocker : BluetoothMessage? = null
    private var m_responseString : String = ""
//    val m_receiveMap = HashMap<String, (() -> Unit)>()

    // Create a BroadcastReceiver for ACTION_FOUND.
    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when(intent.action) {
                BluetoothDevice.ACTION_FOUND -> {
                    onDeviceFound(intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE))
                }
            }
        }
    }


    fun init(activity: MainActivity){
        m_activity = activity

        val filter = IntentFilter(BluetoothDevice.ACTION_FOUND)
        m_activity.registerReceiver(receiver, filter)


        // Bluetooth prüfen
        val ba : BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()
//        m_bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
        if (ba != null) {
            m_bluetoothAdapter = ba
            if (!m_bluetoothAdapter.isEnabled()) {
                // Wenn Bluetooth nicht aktiviert ist, Popup zum aktivieren starten
                val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
                m_activity.startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT)
            }
            else {
                connect()
            }
        } else {
            // Das Gerät unterstützt kein Bluetooth
            Toast.makeText(m_activity, "Ihr Gerät unterstützt kein Bluetooth", Toast.LENGTH_LONG)
                .show()
        }
    }

    fun destroy(){
        m_activity.unregisterReceiver(receiver)
    }

    // Wenn die Verbindung unterbrochen und neu hergestellt werden soll
    fun reinit(){
        m_isConnected = false
        m_isRestart = true
        if (!m_bluetoothAdapter.isEnabled()) {
            // Wenn Bluetooth nicht aktiviert ist, Popup zum aktivieren starten
            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            m_activity.startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT)
        }
        else {
            connect()
        }
    }

    fun addListener(message: BluetoothMessage){
        m_sentList.add(message)
    }

    fun onRequestPermissionsResult(permissions: Array<out String>,
                                   grantResults: IntArray){
        if (grantResults.size == 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            m_bluetoothAdapter.startDiscovery()
        } else {
            Toast.makeText(m_activity, "Bitte stellen Sie eine Verbindung mit dem Gerät \"Air\" her.", Toast.LENGTH_LONG).show()
        }
    }

    fun onDeviceFound(device: BluetoothDevice){
        val deviceName = device.name
//        val deviceHardwareAddress = device.address // MAC address
        Log.e(TAG, "Gefunden: " + deviceName)
        if(deviceName == MICROCONTROLLER_BT_NAME) {
            m_isConnected = true
            val ct = BtcClientThread(device, MY_UUID)
            m_activity.showConnectionError(false)
            createAndStartThread(ct)
            m_bluetoothAdapter.cancelDiscovery()
        }
    }

    // Fehler beim Verbindungsaufbau
    fun onConnectionError(){
        m_isConnected = false
        m_activity.runOnUiThread {
            Toast.makeText(m_activity, "Es konnte keine Verbindung zu \"" + MICROCONTROLLER_BT_NAME + "\" hergestellt werden", Toast.LENGTH_LONG).show()
            m_activity.showConnectionError(true)
        }
    }

    // Fehler in der laufenden Verbindung
    fun onConnectionLost(){
        m_isConnected = false
        m_activity.runOnUiThread {
            Toast.makeText(m_activity, "Die Verbindung zu \"" + MICROCONTROLLER_BT_NAME + "\" wurde unterbrochen", Toast.LENGTH_LONG).show()
            m_activity.showConnectionError(true)
        }
    }

    private fun searchPAMFMicrocontroller(){

        val hasPermission = ActivityCompat.checkSelfPermission(
            m_activity,
            ACCESS_COARSE_LOCATION
        )
        if (hasPermission == PackageManager.PERMISSION_GRANTED) {
            m_bluetoothAdapter.startDiscovery()
            return
        }

        ActivityCompat.requestPermissions(
            m_activity, arrayOf(
                ACCESS_COARSE_LOCATION
            ),
            REQUEST_COARSE_LOCATION_PERMISSIONS
        )

    }

    fun connect(){
        if(m_isConnected)
            return
        val pairedDevices: Set<BluetoothDevice>? = m_bluetoothAdapter.bondedDevices

        var found = false;
        pairedDevices?.forEach { device ->
            val deviceName = device.name
            val deviceHardwareAddress = device.address // MAC address

            MY_UUID = device.uuids[0].uuid
            // Alle UUids ausgeben
            device.uuids.forEach {
                Log.e(TAG, "UUID: " + it)
            }
            Log.e(TAG, "connected: " + deviceName + " addr: " + deviceHardwareAddress)

            if(deviceName == MICROCONTROLLER_BT_NAME) {
                m_isConnected = true
                val ct = BtcClientThread(device, MY_UUID)
                m_activity.showConnectionError(false)
                createAndStartThread(ct)
                found = true
            }
        }
        if(!found){
            searchPAMFMicrocontroller()
        }
    }

    fun sendMessage(message : String){
        // Erzeugt eine einfache BluetoothMessage. Diese ruft von sich aus
        val btm = BluetoothMessage(false, false, message)
    }

    fun sendMessage(message : BluetoothMessage){
        if(blocker == message)
            blocker = null
        m_sendQueue.add(message)
    }


    private fun createAndStartThread(t: BtcClientThread): Thread? {
        if(m_isRestart) {
            m_isRestart = false
            val restartQueue = m_sentList.filter { it.resendOnError } + m_sendQueue.filter { it.resendOnError }
            m_sendQueue.clear()
            m_sentList.clear()
            CogData.isRequestingWeights = false
            EwdData.isRequestingData = false
            RudderData.isRequestingData = false
            restartQueue.forEach { it.onConnectionRestart() }
            m_activity.onConnectionRestart()
        }
        val workerThread: Thread = object : Thread() {
            var keepRunning = true
            override fun run() {
                try {
                    Looper.prepare()
                    t.start()
                    t.join()
                    val socket: BluetoothSocket? = t.socket
                    if (socket != null) {
                        // Output Stream vorbereiten
                        var _os: OutputStream? = null
                        try {
                            _os = socket.outputStream
                        } catch (e: IOException) {
                            Log.e(TAG, null, e)
                        }
                        val os: OutputStream = _os!!

                        //  Test Nachricht
//                        send(os, "1" + "\r")

                        val inputS = socket.inputStream

                        m_activity.runOnUiThread {
                            Toast.makeText(m_activity, "Die Verbindung wurde hergestellt. Daten werden empfangen.", Toast.LENGTH_SHORT).show()
                        }

                        while (keepRunning) {
                            // Senden
                            // Nur senden, wenn es keinen Blocker gibt
                            // TODO: Entfernt am 19.03
                            if(blocker == null) {
                                val sendMsg = m_sendQueue.poll()
                                if (sendMsg != null) {
                                    send(os, sendMsg)
                                }
                            }

                            // Empfangen
                            val txt: String? = receive(inputS)
                            if (txt != null && txt.trim() != "") {
                                Log.w(TAG, "Empfangen: " + txt.trim())
                                m_responseString += txt.trim()
                                // Teilen
                                val split_str = m_responseString.split(";")
                                // Prüfen ob das letzte Teil leer ist (das beduetet, die Nachricht endet mit einem Semikolon)
                                if(split_str[split_str.size-1] == ""){
                                    split_str.forEach{
                                        if(it != ""){
//                                            Log.e(TAG, "Parse all: " + it)
                                            parseMessage(it.trim())
                                        }
                                    }
                                    m_responseString = ""
                                } else {
                                    // Alle bis auf das letzte parsen
                                    split_str.subList(0, split_str.size-1).forEach{
//                                        Log.e(TAG, "Parse only: " + it)
                                        parseMessage(it.trim())
                                    }
                                    // Die Nachricht ohne endendes Semicolon als Start vor den nächsten Empfang setzen
//                                    Log.e(TAG, "append: " + split_str[split_str.size-1])
                                    m_responseString = split_str[split_str.size-1]
                                }

                            }
                        }
                    }
                } catch (e: InterruptedException) {
                    Log.e(TAG, null, e)
                    keepRunning = false
                } catch (e: IOException) {
                    Log.e(TAG, null, e)
                    keepRunning = false
                } finally {
                    Log.d(TAG, "calling cancel() of " + t.getName())
                    t.cancel()
                    onConnectionLost()
                }
            }
        }
        workerThread.start()
        return workerThread
    }

    private fun send(os: OutputStream, message: BluetoothMessage)  {
        try {
            // Response-Listener registrieren
//            m_receiveMap.putAll(message.response)
            if(message.response != null)
                m_sentList.add(message)
            // Nachricht senden
            os.write((message.message + ';').toByteArray())
            Log.w(TAG, "Send: " + message.message + " (" + message.message.toByteArray() + ")")
            // Status der Nachricht auf gesendet setzen
            message.isSent()
            if(message.blocking)
                blocker = message
        } catch (e: IOException) {
            Log.e(TAG, "error while sending", e)
            throw e
        }
    }

    private fun receive(inputStream: InputStream): String? {
        try {
            val num = inputStream.available()
            if (num > 0) {
                val buffer = ByteArray(num)
                val read = inputStream.read(buffer)
                if (read != -1) {
                    return String(buffer, 0, read)
                }
            }
        } catch (e: IOException) {
            Log.e(TAG, "receive()", e)
            throw e
        }
        return null
    }

    fun parseMessage(msg: String){
        if(m_sentList.find { btMessage ->
            var found = false;
            btMessage.response.forEach {
                // String-Gleichheit oder Übereinstimmung mit regulärem Ausdruck
                if(it.first == msg || Pattern.matches(it.first, msg)){
                    btMessage.onResponse(msg, it.second)
                    found = true
                }
            }
            found
        } == null){
            Log.e(TAG, "Kein passender Verarbeiter für Nachricht \"" + msg + "\"")
        }


/*
        val parser : (() -> Unit)? = m_receiveMap[msg]
        if(parser != null){
            parser()
        } else {
            var match = false
            // RegEx Prüfung
            m_receiveMap.forEach{
                if(Pattern.matches(it.key, msg)) {
                    Log.d(TAG, "Match " + it.key)
                    it.value()
                    match = true
                }
            }
            if(match == false)
            {
                Log.d(TAG, "Kein passender Verarbeiter für Nachricht \"" + msg + "\"")
            }
        }*/
    }

    fun unregisterMessageListener(bluetoothMessage : BluetoothMessage){
        m_sentList.remove(bluetoothMessage)
        if(bluetoothMessage == blocker)
            blocker = null
    }

}

