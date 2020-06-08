package de.abg.pamf

import android.app.Application
import de.abg.pamf.data.RudderData
import de.abg.pamf.data.CogData

class App : Application() {
    override fun onCreate() {
        super.onCreate()
        CogData.init(this)
        RudderData.init(this)
//        FakeReceiver.init()
    }

}