 package org.mariotaku.twidere.util

import android.content.SharedPreferences
import android.os.Bundle
import android.text.format.DateFormat
import android.util.Log
import androidx.fragment.app.DialogFragment
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.ktx.logEvent
import org.mariotaku.kpreferences.KIntKey
import org.mariotaku.kpreferences.get
import org.mariotaku.kpreferences.set
import org.mariotaku.twidere.TwidereConstants
import org.mariotaku.twidere.constant.*
import java.util.*

object UseStats {

    val weekDayStrings = listOf(
            "Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday")

    val weekdayToIdx:Map<String, Int> =
            weekDayStrings.associateWith{ weekDayStrings.indexOf(it) }

    lateinit var firebaseLoginstance: FirebaseAnalytics

    var neweststampdict = mutableMapOf<String, Long?>()
    var lastreaddict = mutableMapOf<String, Long?>()

    var lastRequestTimeStamp: Long = 0.toLong()

    //drustz: those are for list reading hisotries.
    fun initTweetHistoryList(preference: SharedPreferences){
        var neweststampDictStr = preference[newestTweetStampKeyForList]
        var lastreadTidDictStr = preference[lastReadTweetStampKeyForList]

        //if not the first time
        if (neweststampDictStr != "") {
            neweststampdict = neweststampDictStr
                    .splitToSequence(",")
                    .map { it.split("=") }
                    .map { it[0].trim() to it[1].trim().toLong() }
                    .toMap()
                    .toMutableMap()

            lastreaddict = lastreadTidDictStr
                    .splitToSequence(",")
                    .map { it.split("=") }
                    .map { it[0].trim() to it[1].trim().toLong() }
                    .toMap()
                    .toMutableMap()
        }
    }

    fun getNewestTweetTimeStampOfList(preference: SharedPreferences, listID: String):Long {
        if (neweststampdict.isNullOrEmpty()) {
            initTweetHistoryList(preference)
        }
        if (!neweststampdict.containsKey(listID)){
            neweststampdict[listID] = 0
        }
        return neweststampdict[listID]!!
    }

    fun getLastTweetHistoryOfList(preference: SharedPreferences, listID: String):Long {
        if (neweststampdict.isNullOrEmpty()) {
            initTweetHistoryList(preference)
        }
        if (!lastreaddict.containsKey(listID)){
            lastreaddict[listID] = 0
        }
        return lastreaddict[listID]!!
    }

    fun setNewestHistoryOfList(preference: SharedPreferences, listID: String,
                               tstamp: Long) {
        if (neweststampdict.isNullOrEmpty()) {
            initTweetHistoryList(preference)
        }
        neweststampdict[listID] = tstamp
    }

    //below are usage status related functions---

    fun updateAllLastTweetHistories(preference: SharedPreferences) {
        if (neweststampdict.isNullOrEmpty()) {
            return
        }
        Log.d("drz", "timestamps: "+ neweststampdict.toString())
        preference.edit().apply {
            this[lastReadTweetStampKeyForList] = neweststampdict.toString().removeSurrounding("{", "}")
            this[newestTweetStampKeyForList] = neweststampdict.toString().removeSurrounding("{", "}")
        }.apply()
        initTweetHistoryList(preference)
    }

    fun getTodayInWeekIdx(): Int {
        val curtime = System.currentTimeMillis()
        val week_today = DateFormat.format("EEEE", Date(curtime)) as String
        return weekdayToIdx[week_today] as Int
    }

    //recording the time brought to front
    fun recordOpenTime(preference : SharedPreferences){
        var opentimes = preference[openTimesKey]
        val curtime = System.currentTimeMillis()
        lastRequestTimeStamp = curtime
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

        val lastRequestTime = Date(lastRequestTimeStamp)
        val week_lastday = DateFormat.format("EEEE", lastRequestTime) as String
        val lastDay = DateFormat.format("dd", lastRequestTime) as String
        val c: Calendar = GregorianCalendar()

        if (today != lastDay) {
            c[Calendar.HOUR_OF_DAY] = 0 //anything 0 - 23
            c[Calendar.MINUTE] = 0
            c[Calendar.SECOND] = 0
        }

        val midnightmillis = c.timeInMillis
        var weeklyUsage = getUseTimeOfAWeek(preference)
//        Log.d("drz", "today usage before: " + weeklyUsage[weekdayToIdx[week_today]!!])

        if (today != lastDay){
            weeklyUsage[weekdayToIdx[week_today]!!] = curtime - midnightmillis
            weeklyUsage[weekdayToIdx[week_lastday]!!] += midnightmillis - lastRequestTime.time

            preference.edit().apply {
                this[openTimeStamp] = midnightmillis+1
                this[openTimesKey] = 0
            }.apply()
        } else {
            weeklyUsage[weekdayToIdx[week_today]!!] += curtime - lastRequestTimeStamp
//            Log.d("drz", "same day, added " + (curtime - lastRequestTimeStamp))
        }

        preference.edit().apply {
            this[weekUsage] = weeklyUsage.joinToString(",")
        }.apply()

        //we update the lastRequestTimeStamp everytime the request is called
        lastRequestTimeStamp = curtime
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
        for (i in 0..6) {
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
                this[timelistPageVisitStats] = 0
                this[timestatusPageVisitStats] = 0
                this[shutDownDialogueStats] = 0
                this[ignoreDialogueStats] = 0
//                this[lastshowUsageDialogTimeStamp] = 0
                this[lastshowESMDialogTimeStamp] = 0

                this[prefKey] = numdiff.coerceAtLeast(0)
                this[lastModificationTimeStamp] = System.currentTimeMillis()
            }.apply()
        }
    }


    fun getTodayUsageStr(preferences: SharedPreferences): String {
        val weekStats = getUseWeeklyTillNow(preferences)
        val todayIdx = getTodayInWeekIdx()

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
        return usageStr
    }

    fun sendFirebaseEvents(sharedPreferences: SharedPreferences){
        if (sharedPreferences[trackUserID] === "") {
            val uuid = UUID.randomUUID().toString()
            sharedPreferences.edit().apply{
                this[trackUserID] = uuid
            }.apply()
            firebaseLoginstance.setUserId(uuid)
        }

        val weekStats = getUseWeeklyTillNow(sharedPreferences)
        val todayIdx = getTodayInWeekIdx()
        val secs = (weekStats[todayIdx]/1000).toInt()

        Log.d("drz", "sendFirebaseEvents: send!")
        firebaseLoginstance.logEvent("UseStats") {
            param("OpenTimes", sharedPreferences[openTimesKey].toLong())
            param("NewTweetConsume", sharedPreferences[newTweetsStats].toLong())
            param("TweetLike", sharedPreferences[likedTweetsStats].toLong())
            param("TweetReply", sharedPreferences[replyTweetsStats].toLong())
            param("RetweetNQuote", sharedPreferences[retweetTweetsStats].toLong())
            param("TweetCompose", sharedPreferences[composeTweetsStats].toLong())
            param("AccFollow", sharedPreferences[followAccountsStats].toLong())
            param("AccUnfollow", sharedPreferences[unfollowAccountsStats].toLong())
            param("ConsumeTime", secs.toLong())
            param("StatPageView", sharedPreferences[timestatusPageVisitStats].toLong())
            param("StatsDialogueExit", sharedPreferences[shutDownDialogueStats].toLong())
            param("StatsDialogueIgnore", sharedPreferences[ignoreDialogueStats].toLong())
            param("Condition", sharedPreferences[expcondition].toLong())
            sharedPreferences.getString(TwidereConstants.KEY_PID, "")?.let { param("userID", it) }
        }
    }

}