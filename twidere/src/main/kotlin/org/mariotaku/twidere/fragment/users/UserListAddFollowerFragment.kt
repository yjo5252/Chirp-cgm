package org.mariotaku.twidere.fragment.users

import android.content.Context
import android.os.Bundle
import android.util.Log
import org.mariotaku.twidere.constant.IntentConstants
import org.mariotaku.twidere.fragment.ParcelableUsersFragment
import org.mariotaku.twidere.loader.users.AbsRequestUsersLoader
import org.mariotaku.twidere.loader.users.UserFriendsLoader
import org.mariotaku.twidere.model.ParcelableUser
import org.mariotaku.twidere.model.UserKey
import org.mariotaku.twidere.view.holder.UserViewHolder

class UserListAddFollowerFragment : ParcelableUsersFragment() {

    interface OnSelectUserHandler {
        fun onUserSelected(users: ArrayList<ParcelableUser>)
    }

    var mSelectedListener: OnSelectUserHandler? = null

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        adapter.userListAddingMode = true
    }

    override fun onCreateUsersLoader(context: Context, args: Bundle, fromUser: Boolean):
            AbsRequestUsersLoader {
        val accountKey = args.getParcelable<UserKey?>(IntentConstants.EXTRA_ACCOUNT_KEY)
        val userKey = args.getParcelable<UserKey?>(IntentConstants.EXTRA_USER_KEY)
        val screenName = args.getString(IntentConstants.EXTRA_SCREEN_NAME)
        return UserFriendsLoader(context, accountKey, userKey, screenName, adapter.getData(),
                fromUser)
    }

    override fun onUserClick(holder: UserViewHolder, position: Int) {
        val user = adapter.getUser(position) ?: return
        adapter.setUserSelection(position, user)
        mSelectedListener?.onUserSelected(ArrayList(adapter.selectedData))
    }

}
