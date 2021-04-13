package org.mariotaku.twidere.util

import android.content.Context
import android.content.SharedPreferences
import android.text.format.DateFormat
import android.util.Log
import okhttp3.*
import org.json.JSONObject
import org.mariotaku.kpreferences.get
import org.mariotaku.kpreferences.set
import org.mariotaku.library.objectcursor.ObjectCursor
import org.mariotaku.sqliteqb.library.Columns.Column
import org.mariotaku.sqliteqb.library.Expression
import org.mariotaku.twidere.Constants
import org.mariotaku.twidere.annotation.CustomTabType
import org.mariotaku.twidere.constant.expcondition
import org.mariotaku.twidere.constant.expconditionChangeTimeStamp
import org.mariotaku.twidere.model.Tab
import org.mariotaku.twidere.model.tab.impl.HomeTabConfiguration
import org.mariotaku.twidere.provider.TwidereDataStore.Tabs
import java.io.IOException
import java.util.*


object DrzUtils {

    //drustz: tab related utils
    fun removeHomeFeedTab(context: Context) {
        val resolver = context.contentResolver
        val where = Expression.equalsArgs(Column(Tabs.TYPE))
        resolver.delete(Tabs.CONTENT_URI, where.sql, arrayOf(CustomTabType.HOME_TIMELINE))
    }

    fun addHomeFeedTab(context: Context) {
        val tabType = CustomTabType.HOME_TIMELINE
        val conf = HomeTabConfiguration()
        val tab = Tab().apply {
            this.type = tabType
            this.icon = conf.icon.persistentKey
            this.position = 0
        }
        val valuesCreator = ObjectCursor.valuesCreatorFrom(Tab::class.java)
        context.contentResolver.insert(Tabs.CONTENT_URI, valuesCreator.create(tab))
    }

    fun removeAllListFeedTabs(context: Context) {
        val resolver = context.contentResolver
        val where = Expression.equalsArgs(Column(Tabs.TYPE))
        resolver.delete(Tabs.CONTENT_URI, where.sql, arrayOf(CustomTabType.LIST_TIMELINE))
    }

    //drustz: the condition transition map
    val transitions = listOf(
            mapOf(3 to 2, 2 to 1, 1 to 0, -1 to 3, 0 to 3),
            mapOf(1 to 3, 3 to 0, 0 to 2, -1 to 1, 2 to 1),
            mapOf(2 to 0, 0 to 3, 3 to 1, -1 to 2, 1 to 2),
            mapOf(0 to 1, 1 to 2, 2 to 3, -1 to 0, 3 to 0))

    fun getTransition(pid: String) : Int {
        val intid = pid.filter { it.isDigit() }.toInt()
        return when (intid){
            in 0..20 -> 0
            in 21..40 -> 1
            in 41..60 -> 2
            else -> 3
        }
    }

    fun changeCondition(prev_cond: Int, pid: String, preferences: SharedPreferences, context: Context){
        var group = getTransition(pid)
        val condition = transitions[group][prev_cond]!!
        var incond = condition.and(2)
        var outcond = condition.and(1)
        preferences.edit().apply{
            this[expcondition] = condition
            this[expconditionChangeTimeStamp] = System.currentTimeMillis()
            putBoolean("shouldShowScreenshotDialog", true)
            putBoolean(Constants.KEY_INTERNAL_FEATURE, incond > 0)
            putBoolean(Constants.KEY_EXTERNAL_FEATURE, outcond > 0)
        }.apply()

        updateConditionChangeToServer(pid, "$prev_cond-$condition")
        Log.d("drz", "pid: $pid, changeCondition from $prev_cond to $condition, in/out: $incond/$outcond")
        //drustz: configure tabs
        if (condition < 2){
            removeAllListFeedTabs(context)
            //first remove if any, then add one
            removeHomeFeedTab(context)
            addHomeFeedTab(context)
        } else if (condition >= 2){
            removeHomeFeedTab(context)
        }
    }

    fun changeToNextCondition(pid: String, preferences: SharedPreferences, context: Context){
        changeCondition(preferences[expcondition], pid, preferences, context)
    }

    fun initConditionWithPID(pid: String, preferences: SharedPreferences, context: Context){
        changeCondition(-1, pid, preferences, context)
    }

    fun checkPidCondExists(pid: String, condition: String, preferences: SharedPreferences) {
        val client = OkHttpClient()
        val jsonObject = JSONObject()
        jsonObject.put("pid", pid)
        jsonObject.put("condition", condition)
        jsonObject.put("quest", "query")
        val body = RequestBody.create(
                MediaType.parse("application/json; charset=utf-8"), jsonObject.toString())

        val request = Request.Builder()
                .method("POST", body)
                .url("https://l84rt6fed7.execute-api.us-west-2.amazonaws.com/default/checkParticipantStatus")
                .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                // Handle this
            }

            override fun onResponse(call: Call, response: Response) {
                try {
                    val jsonData: String = response.body()?.string() ?: ""
                    val Jobject = JSONObject(jsonData)
                    val count = Jobject.get("Count").toString().toInt()
                    Log.d("drz", "onResponse: count $count")
                    if (count > 0) {
                        with(preferences.edit()) {
                            putBoolean("shouldShowScreenshotDialog", false)
                            commit()
                        }
                    }
                } catch (e: Exception) {
                    Log.d("drz", "onResponse: $e")
                }
            }
        })
    }

    fun updateConditionChangeToServer(pid: String, condition: String){
        val now = DateFormat.format("MM/dd-hh:mm:ss aa", Date(System.currentTimeMillis())) as String
        val client = OkHttpClient()
        val jsonObject = JSONObject()
        jsonObject.put("pid", pid)
        jsonObject.put("condition", condition)
        jsonObject.put("time", now)
        jsonObject.put("quest", "update")
        val body = RequestBody.create(
                MediaType.parse("application/json; charset=utf-8"), jsonObject.toString())

        val request = Request.Builder()
                .method("POST", body)
                .url("https://l84rt6fed7.execute-api.us-west-2.amazonaws.com/default/checkParticipantStatus")
                .build()

        client.newCall(request).enqueue(object: Callback {
            override fun onFailure(call: Call, e: IOException) {}
            override fun onResponse(call: Call, response: Response) {}
        })
    }
}