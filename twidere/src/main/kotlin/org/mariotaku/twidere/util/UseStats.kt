package org.mariotaku.twidere.util

import android.content.SharedPreferences
import android.text.format.DateFormat
import android.util.Log
import org.mariotaku.kpreferences.KIntKey
import org.mariotaku.kpreferences.get
import org.mariotaku.kpreferences.set
import org.mariotaku.twidere.Constants
import org.mariotaku.twidere.constant.*
import java.util.*

object UseStats {

    public val weekDayStrings = listOf(
            "Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday")

    public val weekdayToIdx:Map<String, Int> =
            weekDayStrings.associateWith{ weekDayStrings.indexOf(it) }

    fun getTodayInWeekIdx(): Int {
        val curtime = System.currentTimeMillis()
        val week_today = DateFormat.format("EEEE", Date(curtime)) as String
        return weekdayToIdx[week_today] as Int
    }

    //recording the time brought to front
    fun recordOpenTime(preference : SharedPreferences){
        var opentimes = preference[openTimesKey]
        val curtime = System.currentTimeMillis()
        val today = DateFormat.format("dd", Date(curtime) ) as String
        val lastOpenTime = Date(preference[openTimeStamp])
        val lastDay = DateFormat.format("dd", lastOpenTime) as String

        if (today != lastDay) {
            opentimes = 0
        }

        //we need to see if there's any day gaps between last closed and this open
        resetTimeOfWeek(preference)

        preference.edit().apply {
            this[openTimeStamp] = System.currentTimeMillis()
            this[openTimesKey] = opentimes + 1
        }.apply()
    }

    // when it's going background
    fun recordCloseTime(preference: SharedPreferences){
        val weeklyUsage = getUseWeeklyTillNow(preference)
        preference.edit().apply {
            this[weekUsage] = weeklyUsage.joinToString(",")
            this[closeTimeStamp] = System.currentTimeMillis()
        }.apply()
    }


    //drustz: this is to request the use time stas until now.
    //because the current foreground time is not updated in the arraylist
    //Called by the UsageStatsActivity
    //update today's usage time since last open
    fun getUseWeeklyTillNow(preference: SharedPreferences): List<Long>{
        val curtime = System.currentTimeMillis()
        val week_today = DateFormat.format("EEEE", Date(curtime)) as String
        val today = DateFormat.format("dd", Date(curtime)) as String
        val lastOpenTime = Date(preference[openTimeStamp])
        val week_lastday = DateFormat.format("EEEE", lastOpenTime) as String
        val lastDay = DateFormat.format("dd", lastOpenTime) as String
        val c: Calendar = GregorianCalendar()

        if (today != lastDay) {
            c[Calendar.HOUR_OF_DAY] = 0 //anything 0 - 23
            c[Calendar.MINUTE] = 0
            c[Calendar.SECOND] = 0
        }

        val midnightmillis = c.timeInMillis
        val curtimemillis = System.currentTimeMillis()
        var weeklyUsage = getUseTimeOfAWeek(preference)

        if (today != lastDay){
            weeklyUsage[weekdayToIdx[week_today]!!] = curtimemillis - midnightmillis
            weeklyUsage[weekdayToIdx[week_lastday]!!] += midnightmillis - lastOpenTime.time

            preference.edit().apply {
                this[openTimeStamp] = midnightmillis
                this[openTimesKey] = 0
            }.apply()
        } else {
            weeklyUsage[weekdayToIdx[week_today]!!] += curtimemillis - lastOpenTime.time
            Log.d("drz", "same day, added " + (curtimemillis - lastOpenTime.time))
        }

        return weeklyUsage
    }

    //drustz: called when the app goes to foreground
    //check if there's any day not opend the app, clear the usage time
    //and return updated open times
    private fun resetTimeOfWeek(preference: SharedPreferences) {
        val timeSinceLastClose = System.currentTimeMillis()
        val todayInWeek = DateFormat.format("EEEE",
                Date(System.currentTimeMillis())) as String
        val todayWeekIdx = weekdayToIdx[todayInWeek] as Int

        val c: Calendar = GregorianCalendar()
        c[Calendar.HOUR_OF_DAY] = 0 //anything 0 - 23
        c[Calendar.MINUTE] = 0
        c[Calendar.SECOND] = 0
        //today midnight
        val ondaymillis = 1000 * 60 /* minute */ * 60 * 24 //day

        var weeklyUsage = getUseTimeOfAWeek(preference)
        for (i in 1..6) {
            if (c.timeInMillis - i * ondaymillis <= preference[closeTimeStamp]) break
            weeklyUsage[mod(todayWeekIdx-i, 7)] = 0
        }
        preference.edit().apply {
            this[weekUsage] = weeklyUsage.joinToString(",")
        }.apply()
    }

    private fun mod(x: Int, y: Int): Int {
        val result = x % y
        return if (result < 0) result + y else result
    }

    //get 7day's time usage
    private fun getUseTimeOfAWeek(preference: SharedPreferences): MutableList<Long>{
        return preference[weekUsage]
                .trim().splitToSequence(',')
                .map{ it.toLong() }.toMutableList()
    }

    private fun lastModificationIsToday(preference: SharedPreferences) : Boolean {
        val curtime = System.currentTimeMillis()
        val today = DateFormat.format("dd", Date(curtime)) as String
        val lastModificationTime = preference[lastModificationTimeStamp]
        val lastDay = DateFormat.format("dd", lastModificationTime) as String
        return lastDay == today
    }

    fun modifyStatsKeyCount(preference: SharedPreferences, prefKey: KIntKey, numdiff: Int) {
        val prevnewTweets = preference[prefKey]

        if (lastModificationIsToday(preference)) {
            preference.edit().apply {
                this[prefKey] = prevnewTweets + numdiff
                this[lastModificationTimeStamp] = System.currentTimeMillis()
            }.apply()
        } else {
            preference.edit().apply {
                //first reset all stats
                this[newTweetsStats] = 0
                this[composeTweetsStats] = 0
                this[replyTweetsStats] = 0
                this[likedTweetsStats] = 0
                this[retweetTweetsStats] = 0
                this[followAccountsStats] = 0
                this[unfollowAccountsStats] = 0

                this[prefKey] = numdiff.coerceAtLeast(0)
                this[lastModificationTimeStamp] = System.currentTimeMillis()
            }.apply()
        }
    }

}