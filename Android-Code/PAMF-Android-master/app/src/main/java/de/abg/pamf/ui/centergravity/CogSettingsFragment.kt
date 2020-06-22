package de.abg.pamf.ui.centergravity

import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.*
import androidx.fragment.app.Fragment
import de.abg.pamf.MainActivity
import de.abg.pamf.R
import de.abg.pamf.data.CogData
import de.abg.pamf.ui.InfoFragment
import kotlinx.android.synthetic.main.fragment_cog_settings.*

class CogSettingsFragment : Fragment() {

    val TAG = "FRAGMENT_COG_S"

    lateinit var m_rg_type : RadioGroup
    lateinit var m_et_distance_12 : EditText
    lateinit var m_et_height_1 : EditText
    lateinit var m_tv_distance_front : TextView
    lateinit var m_et_distance_front : EditText
    lateinit var m_et_distance_target : EditText

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val root = inflater.inflate(R.layout.fragment_cog_settings, container, false)

        // Setzt das Menu (Zurück bzw. Speichern Button)
        setHasOptionsMenu(true)

        // Speichert die interaktiven Elemente
        m_rg_type = root.findViewById(R.id.cog_s_rg_type)
        m_et_distance_12 = root.findViewById(R.id.cog_s_et_scalesdistance)
        m_et_height_1 = root.findViewById(R.id.cog_s_et_scale1_height)
        m_tv_distance_front = root.findViewById(R.id.cog_s_tv_distancefront)
        m_et_distance_front = root.findViewById(R.id.cog_s_et_distancefront)
        m_et_distance_target = root.findViewById(R.id.cog_s_et_distancetarget)

        // Zeigt die gespeicherten Werte an
        showValues(root)

        // Wechsel des Fahrwerks
        m_rg_type.setOnCheckedChangeListener{ _, checkedId ->
            if(checkedId == R.id.cog_s_rb_type_1)
               CogData.type = 1
            else
               CogData.type = 2
            showValues(root)
        }

        root.findViewById<TextView>(R.id.cog_s_tv_scalesdistance).setOnClickListener {
            val infoFragment = InfoFragment.newInstance("Es ist der Abstand zwischen den Achsen der vorderen und hinteren Waage einzutragen, während sie auf einer ebenen Fläche stehen.", R.drawable.img_cgs_distance12)
            infoFragment.show(fragmentManager, "fragment_alert")
        }
        root.findViewById<TextView>(R.id.cog_s_tv_scale1_heigh).setOnClickListener {
            val infoFragment = InfoFragment.newInstance("Die Höhe von Waage 1 ist so anzupassen, dass die Flügel senkrecht zum Boden sind.", R.drawable.img_cgs_height)
            infoFragment.show(fragmentManager, "fragment_alert")
        }

        m_tv_distance_front.setOnClickListener {
            val infoFragment = InfoFragment.newInstance("Es ist der Abstand zwischen der vorderen Waagen-Achse und der Nase einzutragen.", null)
            infoFragment.show(fragmentManager, "fragment_alert")
        }
        root.findViewById<TextView>(R.id.cog_s_tv_distancetarget).setOnClickListener {
            val infoFragment = InfoFragment.newInstance("Es ist die Distanz zwischen der Nase und dem Ziel-Schwerpunkt einzutragen.", null)
            infoFragment.show(fragmentManager, "fragment_alert")
        }


        return root
    }

    fun showValues(root : View) {
        if(CogData.distance_1_2 != 0)
            m_et_distance_12.setText("" + CogData.distance_1_2)
        if(CogData.height_scale_1 != 0)
            m_et_height_1.setText("" + CogData.height_scale_1)
        if(CogData.distance_front != 0)
            m_et_distance_front.setText("" + CogData.distance_front)
        if(CogData.distance_target != 0)
            m_et_distance_target.setText("" + CogData.distance_target)

        // Unterschiedliche Texte und Bilder zeigen, je nachdem welches Fahrwerk ausgewählt ist
        if(CogData.type == 1) {
            m_tv_distance_front.setText(R.string.cog_s_distancefront_1)
            root.findViewById<ImageView>(R.id.cog_s_iv_type).setImageResource(R.drawable.img_cg_spornfahrwerk)
            m_rg_type.check(R.id.cog_s_rb_type_1)
        } else {
            m_tv_distance_front.setText(R.string.cog_s_distancefront_2)
            root.findViewById<ImageView>(R.id.cog_s_iv_type).setImageResource(R.drawable.img_cg_bugfahrwerk)
            m_rg_type.check(R.id.cog_s_rb_type_2)
        }
    }

    fun saveValues(){
//        Log.e(TAG, "saveData")
        CogData.distance_1_2    = m_et_distance_12.text.toString().toIntOrNull() ?: 0
        CogData.height_scale_1  = m_et_height_1.text.toString().toIntOrNull() ?: 0
        CogData.distance_front  = m_et_distance_front.text.toString().toIntOrNull() ?: 0
        CogData.distance_target = m_et_distance_target.text.toString().toIntOrNull() ?: 0
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
        saveValues()
    }



}