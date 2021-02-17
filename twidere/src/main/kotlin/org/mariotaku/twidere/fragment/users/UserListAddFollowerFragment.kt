package org.mariotaku.twidere.fragment.users

import android.content.Context
import android.os.Bundle
import android.util.Log
import org.mariotaku.kpreferences.get
import org.mariotaku.twidere.constant.IntentConstants
import org.mariotaku.twidere.constant.newDocumentApiKey
import org.mariotaku.twidere.fragment.ParcelableUsersFragment
import org.mariotaku.twidere.loader.users.AbsRequestUsersLoader
import org.mariotaku.twidere.loader.users.UserFriendsLoader
import org.mariotaku.twidere.model.UserKey
import org.mariotaku.twidere.util.IntentUtils
import org.mariotaku.twidere.view.holder.UserViewHolder

class UserListAddFollowerFragment : ParcelableUsersFragment() {

    override fun onCreateUsersLoader(context: Context, args: Bundle, fromUser: Boolean):
            AbsRequestUsersLoader {
        val accountKey = args.getParcelable<UserKey?>(IntentConstants.EXTRA_ACCOUNT_KEY)
        val userKey = args.getParcelable<UserKey?>(IntentConstants.EXTRA_USER_KEY)
        val screenName = args.getString(IntentConstants.EXTRA_SCREEN_NAME)
        return UserFriendsLoader(context, accountKey, userKey, screenName, adapter.getData(),
                fromUser)
    }

    override fun onFollowClicked(holder: UserViewHolder, position: Int) {
        val user = adapter.getUser(position) ?: return
        val accountKey = user.account_key ?: return
        Log.d("drz", "onFollowClicked: here")
    }

    override fun onUserClick(holder: UserViewHolder, position: Int) {
        val user = adapter.getUser(position) ?: return
        adapter.setUserSelection(position, user)
    }

}
