/*
 * 				Twidere - Twitter client for Android
 *
 *  Copyright (C) 2012-2014 Mariotaku Lee <mariotaku.lee@gmail.com>
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.mariotaku.twidere.activity

import android.content.res.TypedArray
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.util.TypedValue
import androidx.annotation.RequiresApi
import com.github.mikephil.charting.charts.BarLineChartBase
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.components.XAxis.XAxisPosition
import com.github.mikephil.charting.components.YAxis
import com.github.mikephil.charting.components.YAxis.YAxisLabelPosition
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.formatter.ValueFormatter
import kotlinx.android.synthetic.main.activity_usagestats.*
import org.mariotaku.kpreferences.get
import org.mariotaku.twidere.R
import org.mariotaku.twidere.constant.*
import org.mariotaku.twidere.util.UseStats


@RequiresApi(Build.VERSION_CODES.LOLLIPOP)
class UsageStatsActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_usagestats)
    }

    override fun onResume() {
        super.onResume()
        updateUsage()
    }

    fun updateUsage() {
        //construct today's usage
        val weekStats = UseStats.getUseWeeklyTillNow(preferences)
        val todayIdx = UseStats.getTodayInWeekIdx()

        var seconds = (weekStats[todayIdx] / 1000).toInt() % 60
        val minutes = (weekStats[todayIdx] / (1000 * 60) % 60).toInt()
        val hours = (weekStats[todayIdx] / (1000 * 60 * 60) % 24).toInt()
        var usageStr = ""
        if (hours > 0){
            usageStr = if (hours > 1) "$hours hrs " else "1 hr "
        }

        if (minutes == 0 && hours > 0) {
            usageStr += "0 min "
        }

        if (minutes > 0){
            usageStr += if (minutes == 1) "1 min " else "$minutes mins "
        }

        var sec = seconds.coerceAtLeast(1)
        usageStr += if (sec == 1) "1 sec" else "$sec secs"
        usingTimeStatsView.text = "⌛ Using Time: " + usageStr

        //today's other stats
        openTimeStatsView.text = "\uD83D\uDEAA App Open Times: ${preferences[openTimesKey]}"
        newTweetsStatsView.text = "\uD83D\uDC24 New Tweets Consumed: ${preferences[newTweetsStats]}"
        likeStatsView.text = "♥ Tweets Liked: ${preferences[likedTweetsStats]}"
        replyStatsView.text = "\uD83D\uDDE8 Tweet Replied: ${preferences[replyTweetsStats]}"
        retweetStatsView.text = "\uD83D\uDD01 Retweeted/Quoted: ${preferences[retweetTweetsStats]}"
        composeStatsView.text = "\uD83D\uDCDD Tweet Composed: ${preferences[composeTweetsStats]}"
        userfollowStatsView.text =
                "\uD83D\uDC40 Accounts Followed: ${preferences[followAccountsStats]}"
        userunfollowStatsView.text =
                "\uD83D\uDD15 Accounts Unfollowed: ${preferences[unfollowAccountsStats]}"

        drawBarChart(weekStats)
    }

    private fun drawBarChart(weekStats: List<Long>) {
        weekStatsChart.setDrawValueAboveBar(true)
        weekStatsChart.setPinchZoom(false)
        weekStatsChart.setDrawGridBackground(false)
        weekStatsChart.isDoubleTapToZoomEnabled = false
        weekStatsChart.isHorizontalScrollBarEnabled = false
        weekStatsChart.isVerticalScrollBarEnabled = false
        weekStatsChart.isDragEnabled = false
        weekStatsChart.legend.isEnabled = false
        weekStatsChart.description.isEnabled = false

        val xAxisFormatter = DayAxisValueFormatter(weekStatsChart)
        val xAxis: XAxis = weekStatsChart.getXAxis()
        xAxis.position = XAxisPosition.BOTTOM
        xAxis.granularity = 1f // only intervals of 1 day
        xAxis.labelCount = 7
        xAxis.setDrawGridLines(false)
        xAxis.valueFormatter = xAxisFormatter
        xAxis.textSize = 16f
        xAxis.textColor = fetchAccentColor()

        val yAxisFormatter = MinuteAxisValueFormatter(weekStatsChart)
        val leftAxis: YAxis = weekStatsChart.axisLeft
        leftAxis.setLabelCount(4, false)
        leftAxis.valueFormatter = yAxisFormatter
        leftAxis.setPosition(YAxisLabelPosition.OUTSIDE_CHART)
        leftAxis.spaceTop = 15f
        leftAxis.axisMinimum = 0f // this replaces setStartAtZero(true)
        leftAxis.textSize = 16f
        leftAxis.textColor = fetchAccentColor()
        leftAxis.granularity = 5f
        leftAxis.isEnabled = false

        weekStatsChart.axisRight.isEnabled = false

        val entries: MutableList<BarEntry> = ArrayList()
        val colors: ArrayList<Int> = ArrayList()
        val todayIdx = UseStats.getTodayInWeekIdx()

        for (i in weekStats.indices) {
            // turn your data into Entry objects
            entries.add(BarEntry(i.toFloat(),
                    (weekStats[1]*i / (1000*60) ).toFloat()))

            if (i <= todayIdx)
                colors.add(Color.parseColor("#16D9DF"))
            else
                colors.add(Color.parseColor("#949C9C"))
        }

        if (weekStatsChart.data != null &&
                weekStatsChart.data.dataSetCount > 0) {
            val set = weekStatsChart.data.getDataSetByIndex(0) as BarDataSet
            set.values = entries
            set.colors = colors
            weekStatsChart.data.notifyDataChanged()
            weekStatsChart.notifyDataSetChanged()
        } else {
            val set = BarDataSet(entries, "Values")
            set.colors = colors

            val data = BarData(set)
            data.setValueTextColor(fetchAccentColor())
            data.setValueTextSize(15f)
            data.barWidth = 0.8f
            data.setValueFormatter(MinuteAxisValueFormatter(weekStatsChart))
            weekStatsChart.data = data
            weekStatsChart.invalidate()
        }
    }

    private fun fetchAccentColor(): Int {
        val typedValue = TypedValue()
        val a: TypedArray = this.obtainStyledAttributes(typedValue.data,
                intArrayOf(android.R.attr.textColorPrimary))
        val color = a.getColor(0, 0)
        a.recycle()
        return color
    }

}

class DayAxisValueFormatter(private val chart: BarLineChartBase<*>) : ValueFormatter() {
    override fun getFormattedValue(value: Float): String {
        return when (value){
            0f -> "Sun"
            1f -> "Mon"
            2f -> "Tue"
            3f -> "Wed"
            4f -> "Thu"
            5f -> "Fri"
            else -> "Sat"
        }
    }
}

class MinuteAxisValueFormatter(private val chart: BarLineChartBase<*>) :
        ValueFormatter() {

    override fun getFormattedValue(value: Float): String {
        val hours = (value / 60).toInt()
        val restmin = (value - hours*60).toInt()

        if (hours >= 1) {
            return if (restmin > 0) "$hours hr $restmin min" else "$hours hr"
        }
        return "${value.toInt()} min"

    }
}