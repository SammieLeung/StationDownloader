package com.station.stationdownloader.contants

const val UNKNOWN_ERROR = -1

enum class ConfigureError {
    INSUFFICIENT_NUMBER_OF_PARAMETERS,
    CONFIGURE_ERROR,
    NOT_SUPPORT_CONFIGURATION
}

enum class TaskExecuteError {
    NOT_SUPPORT_URL,
    ERROR_TORRENT_TASK
}

enum class EngineError {
    ENGINE_INIT_FAILED
}
