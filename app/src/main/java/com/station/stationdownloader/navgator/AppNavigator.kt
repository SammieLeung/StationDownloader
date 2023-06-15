package com.station.stationdownloader.navgator

enum class Destination {
    DOWNLOADING,
    DOWNLOADED,
    SETTINGS
}

interface AppNavigator {
    // Navigate to a given screen.
    fun navigateTo(destination: Destination)
}
