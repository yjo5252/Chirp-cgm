package org.mariotaku.twidere.util

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import org.mariotaku.kpreferences.get
import org.mariotaku.kpreferences.set
import org.mariotaku.microblog.library.MicroBlog
import org.mariotaku.microblog.library.twitter.model.SearchQuery
import org.mariotaku.twidere.constant.popularTweetsCache
import org.mariotaku.twidere.constant.popularTweetsTimeStemp
import org.mariotaku.twidere.extension.model.api.toParcelable
import org.mariotaku.twidere.extension.model.newMicroBlogInstance
import org.mariotaku.twidere.model.AccountDetails
import org.mariotaku.twidere.model.ParcelableStatus
import org.mariotaku.twidere.model.ParcelableTrend
import org.mariotaku.twidere.model.UserKey
import java.util.*

object popularTweets {

    var mtrends = ArrayList<ParcelableTrend>()
    var account: AccountDetails? = null
    var microBlog: MicroBlog? = null
    var mpopTweets = ArrayList<ParcelableStatus>()
    var preference: SharedPreferences? = null

    fun getTrends(twitterWrapper: AsyncTwitterWrapper, accountKey: UserKey, woeId: Int,
                  accountDetails: AccountDetails, context: Context, preferences: SharedPreferences){
        if (mtrends.isNotEmpty()) return
        if (accountDetails == null) return
        mpopTweets.clear()
        account = accountDetails
        preference = preferences
        microBlog = account!!.newMicroBlogInstance(context, MicroBlog::class.java)
        if (!getCache()){
            twitterWrapper.getLocalTrendsAsync(accountKey, woeId)
        }
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
        mpopTweets.clear()
        var maxtrend = mtrends.size
        if (maxtrend > 10){
            maxtrend = 10
        }
        for (i in 0..maxtrend) {
            val queryText = mtrends[i].name
            val searchQuery = SearchQuery(queryText)
            searchQuery.setCount(5)
            searchQuery.setResultType("popular")
            val popularTweets = microBlog?.search(searchQuery)
            if (popularTweets != null) {
                for (t in popularTweets) {
                    mpopTweets.add(t.toParcelable(account!!))
                }
            }
        }
        saveCacheToLocal()
    }

    fun getCache() : Boolean {
        if (preference == null) return false
        val timediff = System.currentTimeMillis() - preference?.get(popularTweetsTimeStemp)!!
        val gson = Gson()
        val type = object : TypeToken<List<ParcelableStatus?>?>() {}.type
        val cachestring = preference?.get(popularTweetsCache)
        if (cachestring != null) {
            mpopTweets = gson.fromJson(cachestring, type)
        }
        //drustz: cache valid for 12hrs
        if (timediff > 60*60*1000*12) {
            return false
        }
        return true
    }

    fun saveCacheToLocal() {
        val gson = Gson()
        preference?.edit().apply {
            this?.set(popularTweetsCache, gson.toJson(mpopTweets))
            this?.set(popularTweetsTimeStemp, System.currentTimeMillis())
        }?.apply()
    }

}