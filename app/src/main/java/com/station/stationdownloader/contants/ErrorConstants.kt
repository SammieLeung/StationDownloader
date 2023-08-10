package com.station.stationdownloader.contants

const val UNKNOWN_ERROR = -1
const val FAILED = 404

enum class ConfigureError {
    CONFIGURE_ERROR,
    INSUFFICIENT_NUMBER_OF_PARAMETERS,
    NOT_SUPPORT_CONFIGURATION,
}

enum class TaskExecuteError {
    ADD_MAGNET_TASK_ERROR,
    DELETE_TASK_FAILED,
    DOWNLOAD_TORRENT_TIME_OUT,
    GET_FILE_SIZE_TIMEOUT,
    GET_HTTP_FILE_HEADER_ERROR,
    INSERT_TORRENT_FILE_INFO_FAILED,
    INSERT_TORRENT_INFO_FAILED,
    NORMAL_TASK_EXISTS,
    NOT_ENOUGH_WORKER_INPUT_ARGUMENTS,
    NOT_SUPPORT_URL,
    NOT_SUPPORT_YET,
    QUERY_TORRENT_FAILED,
    QUERY_TORRENT_FILE_INFO_FAILED,
    QUERY_TORRENT_ID_FAILED,
    REPEATING_TASK_NOTHING_CHANGED,
    SELECT_AT_LEAST_ONE_FILE,
    START_TASK_FAILED,
    START_TASK_URL_TYPE_ERROR,
    STOP_TASK_FAILED,
    SUB_TORRENT_INFO_IS_NULL,
    TASK_COMPLETED,
    TASK_INSERT_ERROR,
    TASK_NOT_FOUND,
    TASK_NOT_RUNNING,
    TASK_NUMBER_REACHED_LIMIT,
    TORRENT_FILE_NOT_FOUND,
    TORRENT_INFO_IS_NULL,
    TORRENT_TASK_EXISTS,
    UPDATE_TASK_CONFIG_FAILED,


}



