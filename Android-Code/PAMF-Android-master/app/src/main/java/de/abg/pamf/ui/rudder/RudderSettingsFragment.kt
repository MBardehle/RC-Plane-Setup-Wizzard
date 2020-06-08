package de.abg.pamf.ui.rudder


import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.EditText
import android.widget.ImageView
import android.widget.RadioGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import de.abg.pamf.MainActivity

import de.abg.pamf.R
import de.abg.pamf.data.RudderData

/**
 * A simple [Fragment] subclass.
 */
class RudderSettingsFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val root = inflater.inflate(R.layout.fragment_rudder_settings, container, false)
        setHasOptionsMenu(true)

        // Zeigt die gespeicherten Werte an
        root.findViewById<EditText>(R.id.rud_s_et_maxangle).setText("" + RudderData.max_diff)
        root.findViewById<EditText>(R.id.rud_s_et_rudderwidth).setText("" + RudderData.rudder_width)

        return root
    }


    override fun onCreateOptionsMenu(menu: Menu?, inflater: MenuInflater) {
        inflater.inflate(R.menu.menu_settings, menu)
        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.navigation_back -> {
                (activity as MainActivity).navController.navigateUp()
                true
            }
            android.R.id.home -> {
                (activity as MainActivity).navController.navigateUp()
                true
            }
            else -> super.onContextItemSelected(item)
        }
    }

    override fun onPause() {
        super.onPause()
        // Eingaben speichern
        RudderData.max_diff =
            this.view!!.findViewById<EditText>(R.id.rud_s_et_maxangle).text.toString().toFloatOrNull()
                ?: 0f
        RudderData.rudder_width =
            this.view!!.findViewById<EditText>(R.id.rud_s_et_rudderwidth).text.toString().toIntOrNull()
                ?: 0
    }
}
