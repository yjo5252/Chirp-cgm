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

import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.annotation.RequiresApi
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
    }

}
