package org.mariotaku.twidere.activity

import android.os.Bundle
import androidx.fragment.app.Fragment
import com.github.appintro.AppIntro
import com.github.appintro.AppIntroFragment
import org.mariotaku.twidere.R

class TutorialClass : AppIntro() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Make sure you don't call setContentView!

        // Call addSlide passing your Fragments.
        // You can use AppIntroFragment to use a pre-built fragment
        addSlide(AppIntroFragment.newInstance(
                title = "Welcome!",
                description = "This is the start of a new week of the study:) We want to show you " +
                        "some features that you may want to use in this week."
        ))


        addSlide(AppIntroFragment.newInstance(
                title = "Create a Twitter List",
                imageDrawable = R.drawable.tutorial_createlist,
                description = "Twitter list is a way to organize your reading experience by " +
                        "adding different accounts in separate lists. Click ''list'' on the " +
                        "side menu and begin to create your own list!"
        ))

        addSlide(AppIntroFragment.newInstance(
                title = "Add Lists To Home Screen",
                imageDrawable = R.drawable.tutorial_addlist,
                description = "You can click the + button on the bottom to add any twitter " +
                        "list to your " +
                        "home bar to organize your reading experience"
        ))

        addSlide(AppIntroFragment.newInstance(
                title = "Review Your Usage Status",
                imageDrawable = R.drawable.tutorial_status,
                description = "Want to know how much time you spent on twitter, " +
                        "or how many tweets " +
                        "you liked today? Click the hourglass button or 'Usage Status' in the " +
                        "side menu to view your usage status!"
        ))

        addSlide(AppIntroFragment.newInstance(
                title = "Filter Out Reply and Retweet",
                imageDrawable = R.drawable.tutorial_timelinefilter,
                description = "Annoyed by the mix of all kinds of tweets in your timeline? " +
                        "Click the filter icon on the top right to hide retweets or replies!"
        ))

        addSlide(AppIntroFragment.newInstance(
                title = "Mute Anything",
                imageDrawable = R.drawable.tutorial_filter,
                description = "You can go to the 'Filter' from the side menu and create " +
                        "filters to hide anything you do not want to see in your timeline!"
        ))

        addSlide(AppIntroFragment.newInstance(
                title = "You've Caught Up",
                imageDrawable = R.drawable.tutorial_history,
                description = "If you see this label in your timeline, that means all " +
                        "the tweets below the label are already seen since last time " +
                        "you opened this app. "
        ))

        addSlide(AppIntroFragment.newInstance(
                title = "That's All!",
                description = "Start your new week's twitter experience in this study by " +
                        "trying out those new features! You can always come back by clicking " +
                        "the 'tutorial' in the side menu."
        ))
    }

    override fun onSkipPressed(currentFragment: Fragment?) {
        super.onSkipPressed(currentFragment)
        // Decide what to do when the user clicks on "Skip"
        finish()
    }

    override fun onDonePressed(currentFragment: Fragment?) {
        super.onDonePressed(currentFragment)
        // Decide what to do when the user clicks on "Done"
        finish()
    }
}