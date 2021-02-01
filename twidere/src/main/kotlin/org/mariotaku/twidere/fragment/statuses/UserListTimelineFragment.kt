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

package org.mariotaku.twidere.fragment.statuses

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AlertDialog
import androidx.loader.content.Loader
import kotlinx.android.synthetic.main.activity_usagestats.*
import org.mariotaku.kpreferences.get
import org.mariotaku.kpreferences.set
import org.mariotaku.twidere.R
import org.mariotaku.twidere.TwidereConstants
import org.mariotaku.twidere.annotation.ReadPositionTag
import org.mariotaku.twidere.constant.IntentConstants.*
import org.mariotaku.twidere.constant.homeTimelineFilterKey
import org.mariotaku.twidere.constant.userTimelineFilterKey
import org.mariotaku.twidere.extension.applyTheme
import org.mariotaku.twidere.extension.onShow
import org.mariotaku.twidere.fragment.BaseDialogFragment
import org.mariotaku.twidere.fragment.ParcelableStatusesFragment
import org.mariotaku.twidere.loader.statuses.UserListTimelineLoader
import org.mariotaku.twidere.model.ParcelableStatus
import org.mariotaku.twidere.model.UserKey
import org.mariotaku.twidere.model.tab.extra.HomeTabExtras
import org.mariotaku.twidere.model.timeline.UserTimelineFilter
import org.mariotaku.twidere.util.UseStats
import org.mariotaku.twidere.util.Utils
import org.mariotaku.twidere.view.holder.TimelineFilterHeaderViewHolder
import java.util.*

/**
 * Created by mariotaku on 14/12/2.
 */
class UserListTimelineFragment : ParcelableStatusesFragment() {

    //drustz : add enable timeline filter on the main feed
    override val enableTimelineFilter: Boolean
        get() = true

    override val timelineFilter: UserTimelineFilter?
        get() = if (enableTimelineFilter) preferences[homeTimelineFilterKey] else null

    override val savedStatusesFileArgs: Array<String>?
        get() {
            val arguments = arguments ?: return null
            val context = context ?: return null
            val accountKey = Utils.getAccountKey(context, arguments)
            val listId = arguments.getString(EXTRA_LIST_ID)
            val userKey = arguments.getParcelable<UserKey?>(EXTRA_USER_KEY)
            val screenName = arguments.getString(EXTRA_SCREEN_NAME)
            val listName = arguments.getString(EXTRA_LIST_NAME)
            val result = ArrayList<String>()
            result.add(TwidereConstants.AUTHORITY_USER_LIST_TIMELINE)
            result.add("account=$accountKey")
            when {
                listId != null -> {
                    result.add("list_id=$listId")
                }
                listName != null -> {
                    if (userKey != null) {
                        result.add("user_id=$userKey")
                    } else if (screenName != null) {
                        result.add("screen_name=$screenName")
                    }
                    return null
                }
                else -> {
                    return null
                }
            }
            return result.toTypedArray()
        }

    override val readPositionTagWithArguments: String?
        get() {
            val arguments = arguments ?: return null
            val tabPosition = arguments.getInt(EXTRA_TAB_POSITION, -1)
            val sb = StringBuilder("user_list_")
            if (tabPosition < 0) return null
            val listId = arguments.getString(EXTRA_LIST_ID)
            val listName = arguments.getString(EXTRA_LIST_NAME)
            when {
                listId != null -> {
                    sb.append(listId)
                }
                listName != null -> {
                    val userKey = arguments.getParcelable<UserKey?>(EXTRA_USER_KEY)
                    val screenName = arguments.getString(EXTRA_SCREEN_NAME)
                    when {
                        userKey != null -> {
                            sb.append(userKey)
                        }
                        screenName != null -> {
                            sb.append(screenName)
                        }
                        else -> {
                            return null
                        }
                    }
                    sb.append('_')
                    sb.append(listName)
                }
                else -> {
                    return null
                }
            }
            return sb.toString()
        }

    override fun onCreateStatusesLoader(context: Context, args: Bundle, fromUser: Boolean):
            Loader<List<ParcelableStatus>?> {
        refreshing = true
//        val extras = arguments?.getParcelable<HomeTabExtras>(EXTRA_EXTRAS)
        val accountKey = Utils.getAccountKey(context, args)
        val listId = args.getString(EXTRA_LIST_ID)
        val userKey = args.getParcelable<UserKey?>(EXTRA_USER_KEY)
        val listName = args.getString(EXTRA_LIST_NAME)
        val screenName = args.getString(EXTRA_SCREEN_NAME)
        //drustz: we disable tab position because the cache can cause problems
        // when the members are modified in this list
        val tabPosition = -1 // args.getInt(EXTRA_TAB_POSITION, -1)
        val loadingMore = args.getBoolean(EXTRA_LOADING_MORE, false)
        //drustz: add filter header
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
        return UserListTimelineLoader(requireActivity(), accountKey, listId, userKey, screenName, listName,
                adapterData, savedStatusesFileArgs, tabPosition, fromUser, loadingMore, extras)
    }

    //drustz: add filter in the header
    override fun onFilterClick(holder: TimelineFilterHeaderViewHolder) {
        val df = UserListTimelineFilterDialogFragment()
        df.setTargetFragment(this, REQUEST_SET_TIMELINE_FILTER)
        fragmentManager?.let { df.show(it, "set_timeline_filter") }
    }

    private fun reloadAllStatuses() {
        adapterData = null
        triggerRefresh()
        showProgress()
    }

    class UserListTimelineFilterDialogFragment : BaseDialogFragment() {

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
                (targetFragment as UserListTimelineFragment).reloadAllStatuses()
            }
            val dialog = builder.create()
            dialog.onShow { it.applyTheme() }
            return dialog
        }

    }

    companion object {
        const val REQUEST_SET_TIMELINE_FILTER = 360
    }
}
