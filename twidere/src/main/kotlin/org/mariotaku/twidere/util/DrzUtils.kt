package org.mariotaku.twidere.util

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import org.mariotaku.kpreferences.get
import org.mariotaku.kpreferences.set
import org.mariotaku.library.objectcursor.ObjectCursor
import org.mariotaku.sqliteqb.library.Expression
import org.mariotaku.sqliteqb.library.Columns.Column
import org.mariotaku.sqliteqb.library.RawItemArray
import org.mariotaku.twidere.Constants
import org.mariotaku.twidere.activity.HomeActivity
import org.mariotaku.twidere.annotation.CustomTabType
import org.mariotaku.twidere.constant.expcondition
import org.mariotaku.twidere.constant.expconditionChangeTimeStamp
import org.mariotaku.twidere.model.Tab
import org.mariotaku.twidere.model.tab.impl.HomeTabConfiguration
import org.mariotaku.twidere.provider.TwidereDataStore.Tabs

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

    fun changeCondition(pid : String, preferences: SharedPreferences, context: Context){
        var incond = 0
        var outcond = 0
        if (pid.contains("in1")){
            incond = 1
        }
        if (pid.contains("ou1")){
            outcond = 1
        }

        val prev_cond = preferences[expcondition]
        val condition = incond * 2 + outcond //0, 1, || 2, 3
        Log.d("drz", "onResume: changed from cond $prev_cond to $condition")

        preferences.edit().apply{
            this[expcondition] = condition
            this[expconditionChangeTimeStamp] = System.currentTimeMillis()
            putBoolean(Constants.KEY_INTERNAL_FEATURE, incond==1)
            putBoolean(Constants.KEY_EXTERNAL_FEATURE, outcond==1)
        }.apply()

        //drustz: configure tabs
        if (condition < 2){
            removeAllListFeedTabs(context)
            if (prev_cond >= 2){
                addHomeFeedTab(context)
            }
        } else if (condition >= 2){
            removeHomeFeedTab(context)
        }
    }
}