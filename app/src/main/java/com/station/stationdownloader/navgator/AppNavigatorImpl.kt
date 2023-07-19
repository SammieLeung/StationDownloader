package com.station.stationdownloader.navgator

import androidx.fragment.app.FragmentActivity
import com.station.stationdownloader.R
import com.station.stationdownloader.ui.fragment.downloading.DownloadingTaskFragment
import com.station.stationdownloader.ui.fragment.SettingsFragment
import javax.inject.Inject

class AppNavigatorImpl @Inject constructor(
    private val activity: FragmentActivity,
) : AppNavigator {
    private var currentScreens: Destination? = null
    override fun navigateTo(screen: Destination) {
        if (screen == currentScreens)
            return
        currentScreens = screen
        val fragment = when (screen) {
            Destination.DOWNLOADING,
            Destination.DOWNLOADED -> {
                DownloadingTaskFragment.newInstance(screen)
            }

            Destination.SETTINGS -> {
                SettingsFragment()
            }

        }

        activity.supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentLayout, fragment)
            .commit()
    }


}