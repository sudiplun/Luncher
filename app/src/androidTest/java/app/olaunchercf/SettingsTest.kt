package app.olaunchercf

import android.content.SharedPreferences
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider.getApplicationContext
import androidx.test.espresso.Espresso
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.scrollTo
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.contrib.RecyclerViewActions
import androidx.test.espresso.matcher.ViewMatchers.hasChildCount
import androidx.test.espresso.matcher.ViewMatchers.hasSibling
import androidx.test.espresso.matcher.ViewMatchers.isClickable
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import app.olaunchercf.data.Constants
import app.olaunchercf.data.Prefs
import app.olaunchercf.ui.AppDrawerAdapter
import org.hamcrest.Matchers.allOf
import org.junit.After
import org.junit.Before
import org.junit.Test

class SettingsTest {

    private lateinit var scenario: ActivityScenario<MainActivity>
    private lateinit var prefs: Prefs

    @Before
    fun clearSharedPreferences() {
        scenario = ActivityScenario.launch(MainActivity::class.java)
        scenario.onActivity { activity ->
            prefs = Prefs(activity)
            val shPrefs: SharedPreferences =
                activity.getSharedPreferences("app.olauncher", 0)
            shPrefs.edit().clear().commit()
        }
    }

    @After
    fun tearDown() {
        scenario.close()
    }

    @Test
    fun testNumberAppsHomeScreen() {
        for (i in 4 until Constants.MAX_HOME_APPS) {
            goToSettings()
            increaseAppNumber()
            Espresso.pressBack()
            onView(withId(R.id.homeAppsLayout)).check(matches(hasChildCount(i + 1)))
        }
        for (i in Constants.MAX_HOME_APPS downTo 1) {
            goToSettings()
            decreaseAppNumber()
            Espresso.pressBack()
            onView(withId(R.id.homeAppsLayout)).check(matches(hasChildCount(i - 1)))
        }
        for (i in 0 until 4) {
            goToSettings()
            increaseAppNumber()
            Espresso.pressBack()
            onView(withId(R.id.homeAppsLayout)).check(matches(hasChildCount(i + 1)))
        }
    }

    @Test
    fun hideClock() {
        goToSettings()
        clickOnSetting(getString(R.string.show_time))
        assert(!prefs.showTime)
    }

    @Test
    fun dateMode() {
        goToSettings()
        clickOnSetting(getString(R.string.show_date))
        onView(withText(getString(R.string.date_ad))).perform(scrollTo(), click())
        assert(prefs.dateMode == Constants.DateMode.AD)

        clickOnSetting(getString(R.string.show_date))
        onView(withText(getString(R.string.off))).perform(scrollTo(), click())
        assert(prefs.dateMode == Constants.DateMode.Off)

        clickOnSetting(getString(R.string.show_date))
        onView(withText(getString(R.string.date_bs))).perform(scrollTo(), click())
        assert(prefs.dateMode == Constants.DateMode.BS)
    }

    @Test
    fun testOpenApp() {
        val actions = listOf(
            R.string.date_click_app,
            R.string.clock_click_app,
            R.string.swipe_left_app,
            R.string.swipe_right_app,
            R.string.swipe_up_app,
            R.string.swipe_down_app,
        )

        goToSettings()

        for (action in actions) {
            clickOnSetting(getString(action))
            onView(withText(getString(R.string.open_app))).perform(scrollTo(), click())
            pickAppFromList()

            val value = when (action) {
                R.string.date_click_app -> {
                    assert(prefs.clickDateAction == Constants.Action.OpenApp)
                    prefs.appClickDate.name
                }
                R.string.clock_click_app -> {
                    assert(prefs.clickClockAction == Constants.Action.OpenApp)
                    prefs.appClickClock.name
                }
                R.string.swipe_left_app -> {
                    assert(prefs.swipeLeftAction == Constants.Action.OpenApp)
                    prefs.appSwipeLeft.name
                }
                R.string.swipe_right_app -> {
                    assert(prefs.swipeRightAction == Constants.Action.OpenApp)
                    prefs.appSwipeRight.name
                }
                R.string.swipe_up_app -> {
                    assert(prefs.swipeUpAction == Constants.Action.OpenApp)
                    prefs.appSwipeUp.name
                }
                R.string.swipe_down_app -> {
                    assert(prefs.swipeDownAction == Constants.Action.OpenApp)
                    prefs.appSwipeDown.name
                }
                else -> {
                    assert(false)
                    return
                }
            }
            assert(value == "Camera") { "Actual value: $value" }
        }
    }

    @Test
    fun testDoubleTap() {
        val actions = listOf(
            R.string.date_click_app,
            R.string.clock_click_app,
            R.string.swipe_left_app,
            R.string.swipe_right_app,
            R.string.swipe_up_app,
            R.string.swipe_down_app,
        )

        for (action in actions) {
            goToSettings()
            clickOnSetting(getString(action))
            onView(withText(getString(R.string.lock_screen))).perform(scrollTo(), click())
        }
    }

    @Test
    fun backupRestore() {
        assert(true)
    }

    @Test
    fun serialize() {
        val prefs = Prefs(getApplicationContext())

        prefs.apply {
            homeAppsNum = 10
            homeAlignment = Constants.Gravity.Center
            swipeLeftAction = Constants.Action.OpenQuickSettings
            dateMode = Constants.DateMode.AD
        }

        val string = prefs.saveToString()

        prefs.firstOpen()
        prefs.firstSettingsOpen()

        clearSharedPreferences()
        prefs.loadFromString(string)

        prefs.apply {
            assert(firstOpen())
            assert(firstSettingsOpen())
            assert(homeAppsNum == 10)
            assert(homeAlignment == Constants.Gravity.Center)
            assert(swipeLeftAction == Constants.Action.OpenQuickSettings)
            assert(dateMode == Constants.DateMode.AD)
        }
    }

    private fun pickAppFromList(i: Int = 4) {
        onView(withId(R.id.recyclerView)).perform(
            RecyclerViewActions.actionOnItemAtPosition<AppDrawerAdapter.ViewHolder>(i, click())
        )
    }

    private fun clickOnSetting(name: String) {
        onView(allOf(hasSibling(withText(name)), isClickable()))
            .perform(scrollTo(), click())
    }

    private fun getString(id: Int): String {
        var value = ""
        scenario.onActivity { value = it.getString(id) }
        return value
    }

    @Test
    fun goToSettings() {
        scenario.onActivity {
            it.openSettings()
        }
        onView(withId(R.id.scrollLayout)).check(matches(isDisplayed()))
    }

    private fun increaseAppNumber() {
        clickOnSetting(getString(R.string.apps_on_home_screen))
        onView(withText("+")).perform(scrollTo(), click())
        onView(withText(getString(R.string.save))).perform(scrollTo(), click())
    }

    private fun decreaseAppNumber() {
        clickOnSetting(getString(R.string.apps_on_home_screen))
        onView(withText("-")).perform(scrollTo(), click())
        onView(withText(getString(R.string.save))).perform(scrollTo(), click())
    }
}
