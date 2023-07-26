package com.station.stationdownloader.contants

const val UNKNOWN_ERROR = -1

enum class ConfigureError {
    CONFIGURE_ERROR,
    INSUFFICIENT_NUMBER_OF_PARAMETERS,
    NOT_SUPPORT_CONFIGURATION,
}

enum class TaskExecuteError {
    ADD_MAGNET_TASK_ERROR,
    DOWNLOAD_TORRENT_TIME_OUT,
    GET_FILE_SIZE_TIMEOUT,
    GET_HTTP_FILE_HEADER_ERROR,
    NORMAL_TASK_EXISTS,
    NOT_ENOUGH_WORKER_INPUT_ARGUMENTS,
    NOT_SUPPORT_URL,
    NOT_SUPPORT_YET,
    REPEATING_TASK_NOTHING_CHANGED,
    SELECT_AT_LEAST_ONE_FILE,
    START_TASK_FAILED,
    START_TASK_URL_TYPE_ERROR,
    SUB_TORRENT_INFO_IS_NULL,
    TASK_INSERT_ERROR,
    TASK_NOT_FOUND,
    TORRENT_INFO_IS_NULL,
    TORRENT_TASK_EXISTS,
    UPDATE_TASK_CONFIG_FAILED,;

}



