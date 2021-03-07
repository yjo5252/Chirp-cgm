package org.mariotaku.twidere.util

import android.content.Context
import android.util.Log
import org.mariotaku.microblog.library.MicroBlog
import org.mariotaku.microblog.library.twitter.model.SearchQuery
import org.mariotaku.twidere.extension.model.api.toParcelable
import org.mariotaku.twidere.extension.model.newMicroBlogInstance
import org.mariotaku.twidere.model.AccountDetails
import org.mariotaku.twidere.model.ParcelableStatus
import org.mariotaku.twidere.model.ParcelableTrend
import org.mariotaku.twidere.model.UserKey
import java.util.ArrayList

object popularTweets {

    var mtrends = ArrayList<ParcelableTrend>()
    var account: AccountDetails? = null
    var microBlog: MicroBlog? = null
    var mpopTweets = ArrayList<ParcelableStatus>()

    fun getTrends(twitterWrapper: AsyncTwitterWrapper, accountKey: UserKey, woeId: Int,
                  accountDetails: AccountDetails, context: Context){
        if (mtrends.isNotEmpty()) return
        mpopTweets.clear()
        twitterWrapper.getLocalTrendsAsync(accountKey, woeId)
        if (accountDetails == null) return
        account = accountDetails
        microBlog = account!!.newMicroBlogInstance(context, MicroBlog::class.java)
    }

    fun setHashTags(trends: ArrayList<ParcelableTrend>){
        if (mtrends.isEmpty()) {
            mtrends = trends.clone() as ArrayList<ParcelableTrend>
            if (mtrends.size > 0){
                getPopularTweetsFromTrends()
            }
        }
    }

    private fun getPopularTweetsFromTrends(){
        var maxtrend = mtrends.size
        if (maxtrend > 10){
            maxtrend = 10
        }
        for (i in 0..maxtrend) {
            val queryText = mtrends[i].name
            val searchQuery = SearchQuery(queryText)
            searchQuery.setCount(10)
            searchQuery.setResultType("popular")
            val popularTweets = microBlog?.search(searchQuery)
            Log.d("drz", "blog? ${microBlog} posting query : $queryText, getsize: ${popularTweets?.size}")
            if (popularTweets != null) {
                for (t in popularTweets) {
                    mpopTweets.add(t.toParcelable(account!!))
                }
            }
        }
    }


}