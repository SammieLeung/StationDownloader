package com.station.stationdownloader.contants

const val UNKNOWN_ERROR = -1

enum class ConfigureError {
    INSUFFICIENT_NUMBER_OF_PARAMETERS,
    CONFIGURE_ERROR,
    NOT_SUPPORT_CONFIGURATION
}

enum class TaskExecuteError {
    NOT_SUPPORT_URL,
    DOWNLOAD_TORRENT_TIME_OUT,
    ADD_MAGNET_TASK_ERROR,
    TORRENT_INFO_IS_NULL,
    SUB_TORRENT_INFO_IS_NULL,
    START_TASK_URL_TYPE_ERROR,
    START_TASK_FAILED,
    GET_FILE_SIZE_TIMEOUT,
}

enum class EngineError {
    ENGINE_INIT_FAILED
}

