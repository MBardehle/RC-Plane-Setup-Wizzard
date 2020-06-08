package de.abg.pamf

import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.View
import android.view.WindowManager
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.NavController
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.bottomnavigation.BottomNavigationView
import de.abg.pamf.remote.BluetoothCommunicator
import de.abg.pamf.remote.BluetoothCommunicator.REQUEST_COARSE_LOCATION_PERMISSIONS
import de.abg.pamf.ui.DataFragment


class MainActivity : AppCompatActivity() {


    lateinit var navController : NavController
    val handler : Handler = Handler()
    lateinit var m_iv_connectionError : ImageView


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val navView: BottomNavigationView = findViewById(R.id.nav_view)

        navController = findNavController(R.id.nav_host_fragment)

        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        val appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.navigation_cog, R.id.navigation_rudder, R.id.navigation_ewd, R.id.navigation_calibrate
            )
        )
        setupActionBarWithNavController(navController, appBarConfiguration)
        navView.setupWithNavController(navController)

        // Bildschirm nicht ausschalten
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        // Bluetooth Fehleranzeige
        m_iv_connectionError = findViewById(R.id.main_iv_connection)
        m_iv_connectionError.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("Verbindungsfehler")
                .setMessage("Die Verbindung wurde unterbrochen. Soll sie wiederhergestellt werden?")
                .setPositiveButton("Ja", { _, _ -> BluetoothCommunicator.reinit() })
                .setNegativeButton("Nein", { _, _ ->  })
                .show()
        }

        // Bluetooth
        BluetoothCommunicator.init(this)
    }


    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        when (requestCode) {
            REQUEST_COARSE_LOCATION_PERMISSIONS -> {
                BluetoothCommunicator.onRequestPermissionsResult(permissions, grantResults)
                return
            }
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    fun onConnectionRestart(){
        if(supportFragmentManager.primaryNavigationFragment is DataFragment){
            (supportFragmentManager.primaryNavigationFragment as DataFragment).onConnectionRestart()
        }
    }

    fun showConnectionError(show: Boolean){
        m_iv_connectionError.visibility = if(show) View.VISIBLE else View.INVISIBLE
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if(requestCode == BluetoothCommunicator.REQUEST_ENABLE_BT){
            Log.e("MAIN", "result code: " + resultCode)
            if(resultCode == 0)
                showConnectionError(true)
            else
                BluetoothCommunicator.connect()
        }
    }

    override fun onDestroy() {
        //Receiver im BTCommunicator beenden
        BluetoothCommunicator.destroy()
        super.onDestroy()
    }
}
