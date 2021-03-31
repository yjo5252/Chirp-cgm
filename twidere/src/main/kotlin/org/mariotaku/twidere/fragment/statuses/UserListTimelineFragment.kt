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
import android.app.usage.UsageStats
import android.content.Context
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AlertDialog
import androidx.core.view.isVisible
import androidx.loader.content.Loader
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.analytics.ktx.logEvent
import kotlinx.android.synthetic.main.activity_usagestats.*
import kotlinx.android.synthetic.main.fragment_content_recyclerview.*
import kotlinx.android.synthetic.main.fragment_status.*
import kotlinx.android.synthetic.main.fragment_status.recyclerView
import org.mariotaku.kpreferences.get
import org.mariotaku.kpreferences.set
import org.mariotaku.ktextension.isNullOrEmpty
import org.mariotaku.twidere.R
import org.mariotaku.twidere.TwidereConstants
import org.mariotaku.twidere.annotation.ReadPositionTag
import org.mariotaku.twidere.constant.*
import org.mariotaku.twidere.constant.IntentConstants.*
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
import org.mariotaku.twidere.util.UseStats.setNewestHistoryOfList
import org.mariotaku.twidere.util.Utils
import org.mariotaku.twidere.view.FixedTextView
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

    override fun readhistoryViewVisible(){
        if (readhistoryShownTimestamp == 0.toLong()) {
            readhistoryShownTimestamp = System.currentTimeMillis()
        }
    }

    override fun setUserVisibleHint(isVisibleToUser: Boolean) {
        super.setUserVisibleHint(isVisibleToUser)

        //drustz: each time the visibility change we collect the time usage
        if (isVisibleToUser) {
            recordEnterTime()
        } else {
            recordLeaveTime()
        }
    }

    private fun recordEnterTime(){
        enterframgmentTimestamp = System.currentTimeMillis()
        readhistoryShownTimestamp = 0
    }

    //drustz: record feed view time after the user move out
    private fun recordLeaveTime(){
        if (recyclerView != null) {
            val firstitm = (recyclerView.layoutManager as LinearLayoutManager).findFirstVisibleItemPosition()
            val lastitm = (recyclerView.layoutManager as LinearLayoutManager).findLastVisibleItemPosition()

            for (i in firstitm..lastitm) {
                val contentview = (recyclerView.layoutManager as
                        LinearLayoutManager).findViewByPosition(i)
                val readhistoryView: FixedTextView? = contentview?.findViewById(R.id.lastReadLabel) as FixedTextView?
                if (readhistoryView != null && readhistoryView.tag == "readhistoryshow") {
                    // drustz: the read history is already shown in the recycler view.
                    //if the readhistoryshowntimestamp is 0, then it means that
                    // the user did not scroll and the history label is already shown
                    // in the first place. So we need to make it same as the enter time
                    if (readhistoryShownTimestamp == 0.toLong()) {
                        readhistoryShownTimestamp = enterframgmentTimestamp
                    }
                }
            }
        }

        val arguments = arguments ?: return
        val listId = arguments.getString(EXTRA_LIST_ID)


        if (listId != null && enterframgmentTimestamp != 0.toLong()) {
            val usetime = (System.currentTimeMillis() - enterframgmentTimestamp) / 1000
            val timeafterhistory = (System.currentTimeMillis() - readhistoryShownTimestamp) / 1000
//            Log.d("drz", "[list] time viewed: "+ usetime)
            if (readhistoryShownTimestamp > 0) {
//                Log.d("drz", "[list] time after readhistory: " + timeafterhistory)
                UseStats.firebaseLoginstance.logEvent("FeedViewTime") {
                    param("FeedViewTimeTotal", usetime)
                    param("FeedViewTimeAfterHistory", timeafterhistory)
                    param("FeedName", listId)
                    param("Condition", preferences[expcondition].toLong())
                    preferences.getString(TwidereConstants.KEY_PID, "")?.let { param("userID", it) }
                }
            } else {
                UseStats.firebaseLoginstance.logEvent("FeedViewTime") {
                    param("FeedViewTimeTotal", usetime)
                    param("FeedName", listId)
                    preferences.getString(TwidereConstants.KEY_PID, "")?.let { param("userID", it) }
                    param("Condition", preferences[expcondition].toLong())
                }
            }
            enterframgmentTimestamp = 0
            readhistoryShownTimestamp = 0
        }
    }

    override fun onResume() {
        super.onResume()
        if (isVisible && userVisibleHint){
            Log.d("drz", "onResume: [list] visible !!")
            recordEnterTime()
        }
    }

    override fun onPause() {
        super.onPause()
        if (isVisible && userVisibleHint){
            Log.d("drz", "onPause: [list] visible !!")
            recordLeaveTime()
        }
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
            isHideTweets = false
        }
        //drustz: use filter only when the internal preference set
        if (preferences.getBoolean(TwidereConstants.KEY_INTERNAL_FEATURE, true)) {
            timelineFilter?.let {
                if (!it.isIncludeReplies) {
                    extras.isHideReplies = true
                }
                if (!it.isIncludeRetweets) {
                    extras.isHideRetweets = true
                }
                if (!it.isIncludeTweets) {
                    extras.isHideTweets = true
                }
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

    //drustz: add read history in the userlist timeline
    override fun onLoadFinished(loader: Loader<List<ParcelableStatus>?>, data: List<ParcelableStatus>?) {
        super.onLoadFinished(loader, data)

        val arguments = arguments ?: return
        val tabPosition = arguments.getInt(EXTRA_TAB_POSITION, -1)
        val listId = arguments.getString(EXTRA_LIST_ID) ?: return
        try {
            //drustz: save the first item in the load for lastread status
            val firstitm = adapter.getStatus(adapter.statusStartIndex, false)
            var newstatscnt = 0

            val lastTstamp = UseStats.getLastTweetHistoryOfList(preferences, listId)
            val neweststamp = UseStats.getNewestTweetTimeStampOfList(preferences, listId)
            if (lastTstamp != 0.toLong()) {
                adapter.lastReadTstamp = lastTstamp
                //we need to calculate the new status from this refresh
                val statscnt = adapter.getStatusCount() - adapter.statusStartIndex

                for (i in adapter.statusStartIndex..statscnt){
                    if (adapter.getStatusTimestamp(i) <= neweststamp)
                        break
                    else newstatscnt += 1
                }
            }
            setNewestHistoryOfList(preferences, listId, firstitm.timestamp)
            //drustz: add to stats
            UseStats.modifyStatsKeyCount(preferences, newTweetsStats, newstatscnt)
        } catch (e: IndexOutOfBoundsException) {

        }
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
                    "tweets" -> filter.isIncludeTweets
                    else -> false
                }
            }
            builder.setTitle(R.string.title_user_timeline_filter)
            builder.setMultiChoiceItems(R.array.entries_home_timeline_filter, checkedItems, null)
            builder.setNegativeButton(android.R.string.cancel, null)
            builder.setPositiveButton(android.R.string.ok) { dialog, _ ->
                dialog as AlertDialog
                val listView = dialog.listView
                val filter = UserTimelineFilter().apply {
                    isIncludeRetweets = listView.isItemChecked(values.indexOf("retweets"))
                    isIncludeReplies = listView.isItemChecked(values.indexOf("replies"))
                    isIncludeTweets = listView.isItemChecked(values.indexOf("tweets"))
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
