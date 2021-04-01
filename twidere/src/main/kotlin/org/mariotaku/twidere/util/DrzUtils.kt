package org.mariotaku.twidere.util

import android.content.Context
import org.mariotaku.library.objectcursor.ObjectCursor
import org.mariotaku.sqliteqb.library.Expression
import org.mariotaku.sqliteqb.library.Columns.Column
import org.mariotaku.sqliteqb.library.RawItemArray
import org.mariotaku.twidere.annotation.CustomTabType
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
}