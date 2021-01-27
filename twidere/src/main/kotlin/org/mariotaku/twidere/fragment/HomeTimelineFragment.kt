/*
 *             Twidere - Twitter client for Android
 *
 *  Copyright (C) 2012-2017 Mariotaku Lee <mariotaku.lee@gmail.com>
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

package org.mariotaku.twidere.fragment

import android.app.Dialog
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AlertDialog
import androidx.loader.content.Loader
import org.mariotaku.kpreferences.get
import org.mariotaku.kpreferences.set
import org.mariotaku.ktextension.isNullOrEmpty
import org.mariotaku.sqliteqb.library.Expression
import org.mariotaku.twidere.R
import org.mariotaku.twidere.TwidereConstants.NOTIFICATION_ID_HOME_TIMELINE
import org.mariotaku.twidere.annotation.FilterScope
import org.mariotaku.twidere.annotation.ReadPositionTag
import org.mariotaku.twidere.constant.IntentConstants.EXTRA_EXTRAS
import org.mariotaku.twidere.constant.homeTimelineFilterKey
import org.mariotaku.twidere.constant.lastReadTweetIDKey
import org.mariotaku.twidere.constant.newestTweetIDKey
import org.mariotaku.twidere.constant.userTimelineFilterKey
import org.mariotaku.twidere.extension.applyTheme
import org.mariotaku.twidere.extension.onShow
import org.mariotaku.twidere.fragment.statuses.UserTimelineFragment
import org.mariotaku.twidere.model.ParameterizedExpression
import org.mariotaku.twidere.model.ParcelableStatus
import org.mariotaku.twidere.model.RefreshTaskParam
import org.mariotaku.twidere.model.UserKey
import org.mariotaku.twidere.model.tab.extra.HomeTabExtras
import org.mariotaku.twidere.model.timeline.TimelineFilter
import org.mariotaku.twidere.model.timeline.UserTimelineFilter
import org.mariotaku.twidere.provider.TwidereDataStore.Statuses
import org.mariotaku.twidere.util.DataStoreUtils
import org.mariotaku.twidere.util.ErrorInfoStore
import org.mariotaku.twidere.view.holder.TimelineFilterHeaderViewHolder
import java.util.*

/**
 * Created by mariotaku on 14/12/3.
 */
class HomeTimelineFragment : CursorStatusesFragment() {

    override val errorInfoKey = ErrorInfoStore.KEY_HOME_TIMELINE

    override val contentUri = Statuses.CONTENT_URI

    override val notificationType = NOTIFICATION_ID_HOME_TIMELINE

    override val isFilterEnabled = true

    override val readPositionTag = ReadPositionTag.HOME_TIMELINE

    override val timelineSyncTag: String?
        get() = getTimelineSyncTag(accountKeys)

    override val filterScopes: Int
        get() = FilterScope.HOME

    //drustz : add enable timeline filter on the main feed
    override val enableTimelineFilter: Boolean
        get() = true

    override val timelineFilter: UserTimelineFilter?
        get() = if (enableTimelineFilter) preferences[homeTimelineFilterKey] else null

    override fun updateRefreshState() {
        val twitter = twitterWrapper
        refreshing = twitter.isStatusTimelineRefreshing(contentUri)
    }

    override fun getStatuses(param: RefreshTaskParam): Boolean {
        if (!param.hasMaxIds) return twitterWrapper.refreshAll(param.accountKeys)
        return twitterWrapper.getHomeTimelineAsync(param)
    }

    override fun setUserVisibleHint(isVisibleToUser: Boolean) {
        super.setUserVisibleHint(isVisibleToUser)
        val context = context
        if (isVisibleToUser && context != null) {
            accountKeys.forEach { accountKey ->
                notificationManager.cancel("home_$accountKey", NOTIFICATION_ID_HOME_TIMELINE)
            }
        }
    }

    //durstz : add header filter for main feed
    override fun processWhere(where: Expression, whereArgs: Array<String>): ParameterizedExpression {
        val arguments = arguments

        if (arguments != null) {
//            val extras = arguments.getParcelable<HomeTabExtras>(EXTRA_EXTRAS)
            val extras = HomeTabExtras().apply {
                isHideQuotes = false
                isHideReplies = false
                isHideRetweets = false
            }
            timelineFilter?.let {
                if (!it.isIncludeReplies) {
                    extras.isHideReplies = true
                }
                if (!it.isIncludeRetweets) {
                    extras.isHideRetweets = true
                }
            }
            if (extras != null) {
                val expressions = ArrayList<Expression>()
                val expressionArgs = ArrayList<String>()
                Collections.addAll(expressionArgs, *whereArgs)
                expressions.add(where)
                DataStoreUtils.processTabExtras(expressions, expressionArgs, extras)
                val expression = Expression.and(*expressions.toTypedArray())
                return ParameterizedExpression(expression, expressionArgs.toTypedArray())
            }
        }
        return super.processWhere(where, whereArgs)
    }

    override fun onLoadFinished(loader: Loader<List<ParcelableStatus>?>, data: List<ParcelableStatus>?) {
        val firstLoad = adapterData.isNullOrEmpty()
        super.onLoadFinished(loader, data)

        try {
            //drustz: save the first item in the load for lastread status
            val firstitm = adapter.getStatus(adapter.statusStartIndex, false)
            val newestid = preferences[newestTweetIDKey]
            var lastreadTid = preferences[lastReadTweetIDKey]
            preferences.edit().apply {
                //only reassign if they are not equal
                if (firstLoad && lastreadTid != newestid) {
                    this[lastReadTweetIDKey] = newestid
                }
                this[newestTweetIDKey] = firstitm.id
            }.apply()
            adapter.lastReadTid = preferences[lastReadTweetIDKey]
        } catch (e: IndexOutOfBoundsException) {

        }
    }

    override fun onFilterClick(holder: TimelineFilterHeaderViewHolder) {
        val df = HomeTimelineFilterDialogFragment()
        df.setTargetFragment(this, REQUEST_SET_TIMELINE_FILTER)
        fragmentManager?.let { df.show(it, "set_timeline_filter") }
    }

    class HomeTimelineFilterDialogFragment : BaseDialogFragment() {

        override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
            val builder = AlertDialog.Builder(requireContext())
            val values = resources.getStringArray(R.array.values_user_timeline_filter)
            val checkedItems = BooleanArray(values.size) {
                val filter = preferences[homeTimelineFilterKey]
                when (values[it]) {
                    "replies" -> filter.isIncludeReplies
                    "retweets" -> filter.isIncludeRetweets
                    else -> false
                }
            }
            builder.setTitle(R.string.title_user_timeline_filter)
            builder.setMultiChoiceItems(R.array.entries_user_timeline_filter, checkedItems, null)
            builder.setNegativeButton(android.R.string.cancel, null)
            builder.setPositiveButton(android.R.string.ok) { dialog, _ ->
                dialog as AlertDialog
                val listView = dialog.listView
                val filter = UserTimelineFilter().apply {
                    isIncludeRetweets = listView.isItemChecked(values.indexOf("retweets"))
                    isIncludeReplies = listView.isItemChecked(values.indexOf("replies"))
                }
                preferences.edit().apply {
                    this[homeTimelineFilterKey] = filter
                }.apply()
                (targetFragment as HomeTimelineFragment).reloadStatuses()
            }
            val dialog = builder.create()
            dialog.onShow { it.applyTheme() }
            return dialog
        }

    }

    companion object {
        const val REQUEST_SET_TIMELINE_FILTER = 361
        fun getTimelineSyncTag(accountKeys: Array<UserKey>): String {
            return "${ReadPositionTag.HOME_TIMELINE}_${accountKeys.sorted().joinToString(",")}"
        }

    }
}

