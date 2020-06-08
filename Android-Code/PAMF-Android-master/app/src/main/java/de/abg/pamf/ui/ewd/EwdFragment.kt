package de.abg.pamf.ui.ewd

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.text.HtmlCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import de.abg.pamf.R
import de.abg.pamf.data.EwdData
import de.abg.pamf.ui.DataFragment

class EwdFragment : Fragment(), DataFragment {


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val root = inflater.inflate(R.layout.fragment_ewd, container, false)

        EwdData.angles.observe(this, Observer<FloatArray> {
            root.findViewById<TextView>(R.id.ewd_tv_angle_a).text = HtmlCompat.fromHtml( getString(R.string.angle, it[0]),
                HtmlCompat.FROM_HTML_MODE_LEGACY
            )
            root.findViewById<TextView>(R.id.ewd_tv_angle_b).text = HtmlCompat.fromHtml( getString(R.string.angle, it[1]),
                HtmlCompat.FROM_HTML_MODE_LEGACY
            )
            root.findViewById<TextView>(R.id.ewd_tv_angle_diff).text = HtmlCompat.fromHtml( getString(R.string.angle, it[2]),
                HtmlCompat.FROM_HTML_MODE_LEGACY
            )
        })

        root.findViewById<TextView>(R.id.ewd_tv_tara_a).setOnClickListener { EwdData.setZero("A")}
        root.findViewById<TextView>(R.id.ewd_tv_tara_b).setOnClickListener { EwdData.setZero("B")}
        return root
    }

    override fun onResume() {
        super.onResume()
        EwdData.requestData()
    }

    override fun onPause() {
        super.onPause()
        EwdData.stopRequestingData()
    }

    override fun onConnectionRestart() {
        EwdData.requestData()
    }
}