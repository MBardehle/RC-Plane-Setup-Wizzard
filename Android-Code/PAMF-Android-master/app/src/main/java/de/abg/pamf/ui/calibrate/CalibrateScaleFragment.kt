package de.abg.pamf.ui.calibrate


import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.lifecycle.Observer
import de.abg.pamf.MainActivity

import de.abg.pamf.R
import de.abg.pamf.data.CogInterface
import de.abg.pamf.data.CogData
import de.abg.pamf.ui.DataFragment

/**
 * A simple [Fragment] subclass.
 */
class CalibrateScaleFragment : Fragment(), CogInterface, DataFragment {

    private var m_selected_scale = "1"
    private var m_scale_factor = 1000

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val root = inflater.inflate(R.layout.fragment_calibrate_scale, container, false)
        setHasOptionsMenu(true)

        // Waage auswählen
        root.findViewById<RadioGroup>(R.id.cal_scale_rg_unit).setOnCheckedChangeListener{ group, checkedId ->
            m_selected_scale = when(checkedId){
                R.id.cal_scale_rb_unit_1 -> "1"
                R.id.cal_scale_rb_unit_2 -> "2"
                R.id.cal_scale_rb_unit_3 -> "3"
                else -> "3"
            }
            loading(true)
            root.findViewById<RadioButton>(R.id.cal_scale_rb_scale1000).isChecked = false
            root.findViewById<RadioButton>(R.id.cal_scale_rb_scale5000).isChecked = false
            root.findViewById<RadioButton>(R.id.cal_scale_rb_scale10000).isChecked = false
            root.findViewById<LinearLayout>(R.id.cal_scale_ll_select_scale).visibility = View.INVISIBLE
            root.findViewById<LinearLayout>(R.id.cal_scale_ll_step1).visibility = View.GONE
            root.findViewById<LinearLayout>(R.id.cal_scale_ll_step2).visibility = View.GONE
            root.findViewById<LinearLayout>(R.id.cal_scale_ll_step3).visibility = View.GONE
            CogData.getStatusOfScale(this, m_selected_scale)
        }

        // Messbereich auswählen
        root.findViewById<RadioGroup>(R.id.cal_scale_rg_scale).setOnCheckedChangeListener{ group, checkedId ->
            when(m_selected_scale){
                "1" -> CogData.scale_1 = when(checkedId){
                    R.id.cal_scale_rb_scale1000 -> 1000
                    R.id.cal_scale_rb_scale5000 -> 5000
                    R.id.cal_scale_rb_scale10000 -> 10000
                    else -> 10000
                }
                "2" -> CogData.scale_1 = when(checkedId){
                    R.id.cal_scale_rb_scale1000 -> 1000
                    R.id.cal_scale_rb_scale5000 -> 5000
                    R.id.cal_scale_rb_scale10000 -> 10000
                    else -> 10000
                }
                "3" -> CogData.scale_1 = when(checkedId){
                    R.id.cal_scale_rb_scale1000 -> 1000
                    R.id.cal_scale_rb_scale5000 -> 5000
                    R.id.cal_scale_rb_scale10000 -> 10000
                    else -> 10000
                }
            }
            root.findViewById<LinearLayout>(R.id.cal_scale_ll_step1).visibility = View.VISIBLE
            root.findViewById<LinearLayout>(R.id.cal_scale_ll_step2).visibility = View.GONE
            root.findViewById<LinearLayout>(R.id.cal_scale_ll_step3).visibility = View.GONE
        }

        // Start
        // -> CG#1#READY;
        root.findViewById<Button>(R.id.cal_scale_btn_step1).setOnClickListener {
            root.findViewById<LinearLayout>(R.id.cal_scale_ll_step1).visibility = View.GONE
            loading(true)
            CogData.startCalibrating(this, m_selected_scale)
            // --> Callback onStartCalibrating
        }

        //Step 2
        // --> CG#1#CAL#Kalibriergewicht in Gramm;
        root.findViewById<Button>(R.id.cal_scale_btn_step2).setOnClickListener {
            val test_weight = this.view!!.findViewById<EditText>(R.id.cal_scale_te_weight).text.toString().toIntOrNull() ?: 0
            if(test_weight > 0){
                loading(true)
                root.findViewById<LinearLayout>(R.id.cal_scale_ll_step2).visibility = View.GONE
                CogData.calibrate(this, m_selected_scale, test_weight)
                // --> Callback onStartCalibrating
            } else {
                Toast.makeText(activity, R.string.cal_scale_toast_error, Toast.LENGTH_LONG).show()
            }
        }

        // Beenden -> Wieder Step 1 zeigen
        root.findViewById<Button>(R.id.cal_scale_btn_step3).setOnClickListener {
            root.findViewById<LinearLayout>(R.id.cal_scale_ll_step1).visibility = View.VISIBLE
            root.findViewById<LinearLayout>(R.id.cal_scale_ll_step2).visibility = View.GONE
            root.findViewById<LinearLayout>(R.id.cal_scale_ll_step3).visibility = View.GONE
        }

        // Gewicht anzeigen
        CogData.weight.observe(this, Observer<IntArray> {
            root.findViewById<TextView>(R.id.cal_scale_tv_weight).text = getString(R.string.cal_scale_weight_value, it[(m_selected_scale.toInt() - 1)].toFloat())
        })
        // Scale Faktor
        CogData.scale_factor.observe(this, Observer<Float> {
            //            Log.e("BOB", "" +  it[(m_selected_scale.toInt() - 1)])
            root.findViewById<TextView>(R.id.cal_scale_tv_factor).text = getString(R.string.cal_scale_factor_value, it)
        })

        // Buttons für Kalibrieranpassung
        root.findViewById<TextView>(R.id.cal_scale_tv_addfactor_m100).setOnClickListener {
            CogData.setFactor(m_selected_scale, "-100")
            CogData.scale_factor.postValue(CogData.scale_factor.value!! - 100)
        }
        root.findViewById<TextView>(R.id.cal_scale_tv_addfactor_m10).setOnClickListener {
            CogData.setFactor(m_selected_scale, "-10")
            CogData.scale_factor.postValue(CogData.scale_factor.value!! - 10)
        }
        root.findViewById<TextView>(R.id.cal_scale_tv_addfactor_p10).setOnClickListener {
            CogData.setFactor(m_selected_scale, "10")
            CogData.scale_factor.postValue(CogData.scale_factor.value!! + 10)
        }
        root.findViewById<TextView>(R.id.cal_scale_tv_addfactor_p100).setOnClickListener {
            CogData.setFactor(m_selected_scale, "100")
            CogData.scale_factor.postValue(CogData.scale_factor.value!! + 100)
        }

        return root
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

    fun loading(loading : Boolean){
        view!!.findViewById<LinearLayout>(R.id.cal_scale_ll_loading).visibility = if(loading) View.VISIBLE else View.GONE
    }

    override fun onStatusRequest(scale: String, weight: Float, maxWeight: Int, cal_factor: Float) {
        activity!!.runOnUiThread {
            view!!.findViewById<TextView>(R.id.cal_scale_tv_weight).text = getString(R.string.cal_scale_weight_value, weight)
//            view!!.findViewById<TextView>(R.id.cal_scale_tv_factor).text = getString(R.string.cal_scale_factor_value, cal_factor)
//            m_scale_factor = cal_factor

            // Scalen anzeigen
            loading(false)
            view!!.findViewById<LinearLayout>(R.id.cal_scale_ll_select_scale).visibility = View.VISIBLE
        }
    }

    override fun onStartCalibrating(scale: String) {
        activity!!.runOnUiThread {
            loading(false)
            view!!.findViewById<LinearLayout>(R.id.cal_scale_ll_step2).visibility = View.VISIBLE
        }
    }

    override fun onCalibrateFinished(scale: String) {
        activity!!.runOnUiThread {
            loading(false)
            view!!.findViewById<LinearLayout>(R.id.cal_scale_ll_step3).visibility = View.VISIBLE

            CogData.requestWeights()
        }
    }

    override fun onConnectionRestart() {
        CogData.requestWeights()
/*        loading(false)
        view!!.findViewById<LinearLayout>(R.id.cal_scale_ll_step1).visibility = View.GONE
        view!!.findViewById<LinearLayout>(R.id.cal_scale_ll_step2).visibility = View.GONE
        view!!.findViewById<LinearLayout>(R.id.cal_scale_ll_step3).visibility = View.GONE*/
    }
}
