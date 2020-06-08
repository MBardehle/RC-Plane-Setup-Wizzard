package de.abg.pamf.remote

import java.util.*
import kotlin.concurrent.schedule

object FakeReceiver {

    val run = true

    fun init() {
        if(run)
            Timer().schedule(2000, 3000) {
/*                when(CogData.scale_1){
                    1000 -> CogData.weight_1.postValue(400 + (round(Math.random() * 600)).toInt())
                    5000 -> CogData.weight_1.postValue(1000 + (round(Math.random() * 4000)).toInt())
                    10000 -> CogData.weight_1.postValue(1200 + (round(Math.random() * 8800)).toInt())
                }*/
                /*
                CogData.weight_1.postValue(50)
                CogData.weight_2.postValue(100)
                CogData.weight_3.postValue(100)*/

            }
    }

}