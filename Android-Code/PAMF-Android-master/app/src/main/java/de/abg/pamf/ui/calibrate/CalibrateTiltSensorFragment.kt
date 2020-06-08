package de.abg.pamf.ui.calibrate

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.core.view.children
import de.abg.pamf.MainActivity

import de.abg.pamf.R
import de.abg.pamf.data.CalibrateEwdData
import de.abg.pamf.ui.DataFragment


class CalibrateTiltSensorFragment : Fragment(), DataFragment {
    var m_sensor = 'X'
    lateinit var m_step_textview : TextView
    lateinit var m_step_imgview : ImageView
    lateinit var m_step_button : Button
    lateinit var m_step_container : LinearLayout
    lateinit var m_loading_container : LinearLayout
    lateinit var m_radioGroup : RadioGroup
    var m_current_step = 0

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val root = inflater.inflate(R.layout.fragment_calibrate_tilt_sensor, container, false)
        setHasOptionsMenu(true)

        // Interaktive / Dynamische Elemente
        m_step_container = root.findViewById(R.id.cal_tilt_ll_steps)
        m_step_textview = m_step_container.findViewById(R.id.cal_tilt_tv_steptext)
        m_step_imgview = m_step_container.findViewById(R.id.cal_tilt_img_stepimg)
        m_step_button = m_step_container.findViewById(R.id.cal_tilt_btn_next_step)

        m_loading_container = root.findViewById(R.id.cal_tilt_ll_loading)

        m_radioGroup = root.findViewById(R.id.cal_tilt_rg_unit)

        // Auswahl des Sensors
        m_radioGroup.setOnCheckedChangeListener{ _, checkedId ->
            Log.e("TAG", "EWD : cal_tilt_rg_unit setOnCheckedChangeListener")
            m_sensor = when(checkedId){
                R.id.cal_tilt_rb_unit_1 -> 'A'
                R.id.cal_tilt_rb_unit_2 -> 'B'
                else -> 'X'
            }
            if(m_sensor != 'X')
                onNextStep(0)
        }

        m_step_button.setOnClickListener {
            if (m_sensor != 'X'){
                Log.e("TAG", "EWD : m_step_button setOnClickListener")
                m_step_container.visibility = View.GONE
                loading(true)
                if(m_current_step == 0)
                    CalibrateEwdData.start(this, m_sensor)
                else
                    CalibrateEwdData.nextStep(this, m_sensor)
            }
            // --> Callback onNextStep
        }


        return root
    }

    fun onNextStep(step: Int){
        activity!!.runOnUiThread {
            Log.e("TAG", "EWD : onNextStep")
            loading(false)
            // Den Text für den folgenden Schritt anzeigen
            when(step){
                0 -> {
                    m_step_textview.setText(R.string.cal_tilt_step1)
                    m_step_imgview.setImageResource(R.drawable.img_sensor_step1)
                }
                1 -> {
                    m_step_textview.setText(R.string.cal_tilt_step2)
                    m_step_imgview.setImageResource(R.drawable.img_sensor_step2)
                }
                2 -> {
                    m_step_textview.setText(R.string.cal_tilt_step3)
                    m_step_imgview.setImageResource(R.drawable.img_sensor_step3)
                }
                3 -> {
                    m_step_textview.setText(R.string.cal_tilt_step4)
                    m_step_imgview.setImageResource(R.drawable.img_sensor_step4)
                }
                4 -> {
                    m_step_textview.setText(R.string.cal_tilt_step5)
                    m_step_imgview.setImageResource(R.drawable.img_sensor_step5)
                }
                5 -> {
                    m_step_textview.setText(R.string.cal_tilt_step6)
                    m_step_imgview.setImageResource(R.drawable.img_sensor_step6)
                }
                6 -> {
                    m_step_textview.setText(R.string.cal_tilt_step7)
                    m_step_imgview.setImageResource(R.drawable.img_sensor_step1)
                }
            }
            m_step_container.visibility = View.VISIBLE
            m_step_button.visibility = View.VISIBLE
            m_current_step = step

            // Die Radio Buttons deaktivieren, wenn ein Prozess noch im Gange ist
            m_radioGroup.children.forEach {
                it.isEnabled = m_current_step == 0
            }
        }
    }

    fun onCalibrationFinished(){
        activity!!.runOnUiThread {
            Log.e("TAG", "EWD : onCalibration Finished")
            loading(false)
            m_step_textview.setText(R.string.cal_tilt_step8)
            m_step_imgview.setImageResource(R.drawable.img_sensor_step1)
//            Toast.makeText(activity, R.string.cal_tilt_step8, Toast.LENGTH_LONG).show()
            m_current_step = 0
            m_step_container.visibility = View.VISIBLE
            m_step_button.visibility = View.INVISIBLE
            // Die Radio Buttons wieder aktivieren
            m_sensor = 'X'
            m_radioGroup.children.forEach {
                it.isEnabled = true
            }
            m_radioGroup.clearCheck()
        }
    }


    fun loading(loading : Boolean){
        m_loading_container.visibility = if(loading) View.VISIBLE else View.GONE
    }


    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                (activity as MainActivity).navController.navigateUp()
                true
            }
            else -> super.onContextItemSelected(item)
        }
    }

    override fun onConnectionRestart() {

    }

}
