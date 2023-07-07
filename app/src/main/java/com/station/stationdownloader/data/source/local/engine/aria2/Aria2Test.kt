package com.station.stationdownloader.data.source.local.engine.aria2

class Aria2Test {
}


enum class Aria2Method(val method: String) {
    TELL_STATUS("aria2.tellStatus"),
    TELL_ACTIVE("aria2.tellActive"),
    TELL_WAITING("aria2.tellWaiting"),
    TELL_STOPPED("aria2.tellStopped"),
    UNPAUSE("aria2.unpause"),
    REMOVE("aria2.remove"),
    FORCE_PAUSE("aria2.forcePause"),
    FORCE_REMOVE("aria2.forceRemove"),
    REMOVE_RESULT("aria2.removeDownloadResult"),
    GET_VERSION("aria2.getVersion"),
    PAUSE_ALL("aria2.pauseAll"),
    GET_SESSION_INFO("aria2.getSessionInfo"),
    SAVE_SESSION("aria2.saveSession"),
    UNPAUSE_ALL("aria2.unpauseAll"),
    FORCE_PAUSE_ALL("aria2.forcePauseAll"),
    PURGE_DOWNLOAD_RESULTS("aria2.purgeDownloadResult"),
    PAUSE("aria2.pause"),
    LIST_METHODS("system.listMethods"),
    GET_GLOBAL_STATS("aria2.getGlobalStat"),
    GET_GLOBAL_OPTIONS("aria2.getGlobalOption"),
    CHANGE_GLOBAL_OPTIONS("aria2.changeGlobalOption"),
    ADD_URI("aria2.addUri"),
    ADD_TORRENT("aria2.addTorrent"),
    ADD_METALINK("aria2.addMetalink"),
    GET_SERVERS("aria2.getServers"),
    GET_PEERS("aria2.getPeers"),
    GET_DOWNLOAD_OPTIONS("aria2.getOption"),
    GET_FILES("aria2.getFiles"),
    CHANGE_POSITION("aria2.changePosition"),
    CHANGE_DOWNLOAD_OPTIONS("aria2.changeOption")
}

