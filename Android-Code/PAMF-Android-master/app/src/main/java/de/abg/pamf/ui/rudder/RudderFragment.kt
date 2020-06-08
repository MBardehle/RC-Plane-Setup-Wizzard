package de.abg.pamf.ui.rudder

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.util.Log
import androidx.core.text.HtmlCompat
import android.view.*
import android.widget.RadioGroup
import android.widget.TextView
import androidx.core.text.HtmlCompat.FROM_HTML_MODE_LEGACY
import androidx.fragment.app.Fragment
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import de.abg.pamf.R
import de.abg.pamf.data.RudderData
import java.util.*
import kotlin.collections.ArrayList
import kotlin.concurrent.schedule
import androidx.lifecycle.Observer
import com.github.mikephil.charting.formatter.DefaultFillFormatter
import de.abg.pamf.MainActivity
import de.abg.pamf.ui.DataFragment
import kotlin.math.abs


class RudderFragment : Fragment(), DataFragment {

    var entries: List<Entry> = ArrayList()

    private val TAG = "RUDDER_FRA"
    private lateinit var chart : LineChart
    private var recording = false
    private var recordingStartTime = 0L
    private var recordedValues : Array<ArrayList<Entry>> = arrayOf(ArrayList<Entry>(),ArrayList<Entry>(),ArrayList<Entry>())
    private var diffColors = ArrayList<Int>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val root = inflater.inflate(R.layout.fragment_rudder, container, false)
        setHasOptionsMenu(true)

        RudderData.angles.observe(this, Observer<FloatArray> {
            root.findViewById<TextView>(R.id.rudder_tv_angle_a).text = HtmlCompat.fromHtml( getString(R.string.angle, it[0]), FROM_HTML_MODE_LEGACY)
            root.findViewById<TextView>(R.id.rudder_tv_angle_b).text = HtmlCompat.fromHtml( getString(R.string.angle, it[1]), FROM_HTML_MODE_LEGACY)
            root.findViewById<TextView>(R.id.rudder_tv_angle_diff).text = HtmlCompat.fromHtml( getString(R.string.angle, it[2]), FROM_HTML_MODE_LEGACY)
            if(abs(it[2]) > RudderData.max_diff){
                root.findViewById<TextView>(R.id.rudder_tv_angle_diff).background = ColorDrawable(resources.getColor(R.color.red))
            } else {
                root.findViewById<TextView>(R.id.rudder_tv_angle_diff).background = resources.getDrawable(R.drawable.bg_border_bot)
            }
            if(recording){
                addDataSet(it)
            }
        })
        RudderData.rudder_ways.observe(this, Observer<IntArray> {
            root.findViewById<TextView>(R.id.rudder_tv_way_a).text = HtmlCompat.fromHtml( getString(R.string.mm, it[0]), FROM_HTML_MODE_LEGACY)
            root.findViewById<TextView>(R.id.rudder_tv_way_b).text = HtmlCompat.fromHtml( getString(R.string.mm, it[1]), FROM_HTML_MODE_LEGACY)
            root.findViewById<TextView>(R.id.rudder_tv_way_diff).text = HtmlCompat.fromHtml( getString(R.string.mm, it[2]), FROM_HTML_MODE_LEGACY)
        })

        root.findViewById<RadioGroup>(R.id.rudder_rg_type).check(if(RudderData.mode_reverse)R.id.rudder_rb_type_2 else R.id.rudder_rb_type_1 )

        root.findViewById<RadioGroup>(R.id.rudder_rg_type).setOnCheckedChangeListener {
            _, checkedId -> RudderData.mode_reverse = checkedId == R.id.rudder_rb_type_2
        }

        root.findViewById<TextView>(R.id.rudder_tv_tara_a).setOnClickListener { RudderData.setZero("A")}
        root.findViewById<TextView>(R.id.rudder_tv_tara_b).setOnClickListener { RudderData.setZero("B")}


        root.findViewById<TextView>(R.id.rudder_tv_record).setOnClickListener {
            recording = !recording
            if(recording){
                resetDiagram()
                (it as TextView).text = getString(R.string.rudder_record_stop)
                recordingStartTime = System.currentTimeMillis()
            } else {
                (it as TextView).text = getString(R.string.rudder_record)
            }
        }

        initDiagram(root)

        return root
    }

    private fun addDataSet(values: FloatArray){
        val time = ((System.currentTimeMillis() - recordingStartTime)/1000f)
        recordedValues[0].add(Entry(time, values[0]))
        recordedValues[1].add(Entry(time, values[1]))
        addDiffValue(Entry(time, values[2]*10))



        // Achsen aktualisieren
        val maxVal = maxOf(values[0], values[1], values[2]*10)
        val minVal = minOf(values[0], values[1], values[2]*10)

        if(time > chart.xAxis.axisMaximum )
            chart.xAxis.axisMaximum = ((time+10).toInt()/10)*10f
        if(maxVal > chart.axisLeft.axisMaximum )
            chart.axisLeft.axisMaximum = ((maxVal+10).toInt()/10)*10f
        if(minVal < chart.axisLeft.axisMinimum )
            chart.axisLeft.axisMinimum = ((minVal-10).toInt()/10)*10f

//        updateDiagram()
    }

    private fun addDiffValue(newEntry : Entry){
        val last = recordedValues[2].last()
        val diffX10 = RudderData.max_diff*10

        // Vorher größer max_diff und danach größer max_diff, oder vorher kleiner max_diff und danach auch kleiner max_diff
        if((last.y > diffX10 && newEntry.y > diffX10) || (last.y < -diffX10 && newEntry.y < -diffX10) ){
            recordedValues[2].add(newEntry)
            diffColors.add(Color.RED)
        }
        // Vorher und nachher zwischen max_diff und -max_diff
        else if(last.y < diffX10 && last.y > -diffX10 && newEntry.y < diffX10 && newEntry.y > -diffX10){
            recordedValues[2].add(newEntry)
            diffColors.add(Color.LTGRAY)
        }
        // Es wird mindestens einmal ein Grenzwert überschritten
        else {
            // Steigung der Linie
            val gradient = (newEntry.y - last.y) / (newEntry.x - last.x)
            // Vorher kleiner als -diffX10 (und danach nicht mehr)
            if(last.y < -diffX10) {
                val x_limit = last.x + (( - diffX10 - last.y) / gradient)
                recordedValues[2].add(Entry(x_limit, -diffX10))
                diffColors.add(Color.RED)
            }
            // Nachher größer als diffX10 (und davor kleiner)
            if(newEntry.y > diffX10) {
                val x_limit = newEntry.x + ((diffX10 - newEntry.y) / gradient)
                recordedValues[2].add(Entry(x_limit, diffX10))
                diffColors.add(Color.LTGRAY)
            }

            // Vorher größer als diffX10
            if(last.y > diffX10) {
                val x_limit = last.x + (( diffX10 - last.y) / gradient)
                recordedValues[2].add(Entry(x_limit, diffX10))
                diffColors.add(Color.RED)
            }
            // Nachher kleiner als -diffX10 (und davor größer)
            if(newEntry.y < -diffX10) {
                val x_limit = newEntry.x + (( - diffX10 - newEntry.y) / gradient)
                recordedValues[2].add(Entry(x_limit, -diffX10))
                diffColors.add(Color.LTGRAY)
            }

            if(newEntry.y < -diffX10 || newEntry.y > diffX10){
                diffColors.add(Color.RED)
            } else {
                diffColors.add(Color.LTGRAY)
            }

            recordedValues[2].add(newEntry)
        }

    }

/*    private fun addDiffValues(newEntry : Entry) {
        val last = recordedValues[2].last()
        val limit = 50f
        if(last.y < limit && newEntry.y > limit  ){
            val gradient = (newEntry.y - last.y) / (newEntry.x - last.x)
            val x_border = last.x + ((limit - last.y) / gradient)
            recordedValues[2].add(Entry(x_border, limit))
            diffColors.add(Color.LTGRAY)
            diffColors.add(Color.RED)
        }
        // Vorher größer, jetzt kleiner
        else if(last.y > limit && newEntry.y < limit) {
            val gradient = (newEntry.y - last.y) / (newEntry.x - last.x)
            val x_border = last.x + ((limit - last.y) / gradient)
            recordedValues[2].add(Entry(x_border, limit))
            diffColors.add(Color.RED)
            diffColors.add(Color.LTGRAY)
        }else if(last.y > limit ){
            diffColors.add(Color.RED)
        } else {
            diffColors.add(Color.LTGRAY)
        }
        recordedValues[2].add(newEntry)
    }*/

    private fun updateDiagram(){
        // Daten aktualisieren
        val dataSet1 = LineDataSet(recordedValues[0], "Sensor A")
        dataSet1.color = Color.BLUE
        setDefaultStyles(dataSet1)
        val dataSet2 = LineDataSet(recordedValues[1], "Sensor B")
        dataSet2.color = Color.GREEN
        setDefaultStyles(dataSet2)
        val dataSet3 = LineDataSet(recordedValues[2], "Differenz (*10)")
        dataSet3.color = Color.RED
        setDefaultStyles(dataSet3)
        dataSet3.setColors(diffColors)
//        setColorsForDiff(dataSet3)


        val lineData = LineData(dataSet1, dataSet2, dataSet3)
        chart.data = lineData
        chart.invalidate()
    }

    private fun setColorsForDiff(dataSet : LineDataSet){
        val colors = ArrayList<Int>()
        val run = recordedValues[2].size
        recordedValues[2].forEach {
            if(it.y < RudderData.max_diff * 10f)
                colors.add(Color.LTGRAY)
            else
                colors.add(Color.RED)
            Log.e("GRAPH", "Run: " + run + " y:" + it.y + " c: " + if(it.y < RudderData.max_diff * 10f)  "LTGREY" else "RED" )
        }
        dataSet.setColors(colors)


    }

    private fun setDefaultStyles(dataSet : LineDataSet){
        dataSet.setDrawCircles(false)
        dataSet.setDrawValues(false)
//        dataSet.valueTextSize = 10f
//        dataSet.isHighlightEnabled = false
    }

    private fun resetDiagram(){
        // Daten zurücksetzen
        recordedValues = arrayOf(ArrayList<Entry>(),ArrayList<Entry>(),ArrayList<Entry>())

        recordedValues[0].add(Entry(0f,0f))
        val dataSet1 = LineDataSet(recordedValues[0], "Sensor A")
        dataSet1.color = Color.BLUE
        setDefaultStyles(dataSet1)

        recordedValues[1].add(Entry(0f,0f))
        val dataSet2 = LineDataSet(recordedValues[1], "Sensor B")
        dataSet2.color = Color.GREEN
        setDefaultStyles(dataSet2)

        recordedValues[2].add(Entry(0f,0f))
        val dataSet3 = LineDataSet(recordedValues[2], "Diff")
        dataSet3.color = Color.RED
        setDefaultStyles(dataSet3)

        diffColors = ArrayList<Int>()
//        diffColors.add(Color.LTGRAY)

        chart.data = LineData(dataSet1, dataSet2, dataSet3)

        // Gestaltung
        chart.setBorderWidth(2f)

        // X-Achse
        chart.xAxis.axisMinimum = 0f
        chart.xAxis.axisMaximum = 20f
        chart.xAxis.position = XAxis.XAxisPosition.BOTTOM

        // Y-Achse
        chart.axisLeft.axisMinimum = -100f
        chart.axisLeft.axisMaximum = 100f
        chart.axisRight.isEnabled = false

        chart.legend.isEnabled = false

        chart.description.isEnabled = false

        chart.invalidate() // refresh

    }

    private fun initDiagram(root : View){
        chart = root.findViewById(R.id.rudder_canvas) as LineChart

        resetDiagram()
        var x = 4f

        // Nur alle 200ms malen, da immer das komplette Diagramm neu gezeichnet wird und dies sonst das Gerät zu stark auslastet
        Timer().schedule(2000, 100) {
            activity?.runOnUiThread(
                fun()  {
                    if(recording){
/*
                        val nearVal = if(x%100 < 50) x%100f else 100-x%100f
                        val val1 = (nearVal + Math.random() * 2f).toFloat()
                        val val2 = (nearVal + Math.random() * 2f).toFloat()
                        val val3 = (val1 - val2) * 10f
                        addDataSet(floatArrayOf(val1, val2, val3))
                        ++x*/
                        if(recordedValues[2].size > 1)
                            updateDiagram()
                    }
                }
            )
        }
    }

    override fun onResume() {
        super.onResume()
        RudderData.requestData()
    }

    override fun onPause() {
        super.onPause()
        RudderData.stopRequestingData()
    }

    override fun onCreateOptionsMenu(menu: Menu?, inflater: MenuInflater) {
        inflater.inflate(R.menu.menu_rud, menu)
        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.navigation_rud_settings -> {
                (activity as MainActivity).navController.navigate(R.id.navigation_rud_settings)
                true
            }
            else -> super.onContextItemSelected(item)
        }
    }

    override fun onConnectionRestart() {
        RudderData.requestData()
    }
}