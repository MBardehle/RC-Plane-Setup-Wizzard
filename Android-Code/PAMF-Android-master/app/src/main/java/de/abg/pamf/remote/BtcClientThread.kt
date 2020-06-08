package de.abg.pamf.remote

import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.util.Log
import java.io.IOException
import java.util.*


class BtcClientThread(device: BluetoothDevice, uuid: UUID?) : Thread() {
    var socket: BluetoothSocket? = null
        private set


    companion object {
        private val TAG = BtcClientThread::class.java.simpleName
    }

    init {
        setName(TAG)
        try {
            socket = device.createRfcommSocketToServiceRecord(uuid)
        } catch (e: IOException) {
            BluetoothCommunicator.onConnectionError()
            Log.e(
                TAG,
                "createRfcommSocketToServiceRecord() failed",
                e
            )
        }
    }

    override fun run() {
        try {
            socket!!.connect()
            Log.d(TAG, "socket connect")
        } catch (connectException: IOException) {
            BluetoothCommunicator.onConnectionError()
            Log.e(
                TAG,
                "createRfcommSocketToServiceRecord() failed 2"
            )
            cancel()
        }
    }

    fun cancel() {
        if (socket != null) {
            try {
                socket!!.close()
            } catch (e: IOException) {
                Log.e(
                    TAG,
                    "Could not close client socket",
                    e
                )
            } finally {
                socket = null
            }
        }
    }

}