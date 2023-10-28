package com.station.stationdownloader.data.source.local.engine.aria2.connection.common

import com.gianlu.aria2lib.commonutils.Prefs

class Aria2UserPK {
    companion object{
        val A2_NETWORK_TIMEOUT: Prefs.KeyWithDefault<Int> = Prefs.KeyWithDefault("a2_networkTimeout", 5)
        val LAST_USED_PROFILE: Prefs.Key = Prefs.Key("lastUsedProfile")
    }
}