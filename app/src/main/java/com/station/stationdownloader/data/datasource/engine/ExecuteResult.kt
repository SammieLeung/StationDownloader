package com.station.stationdownloader.data.datasource.engine

/**
 * author: Sam Leung
 * date:  2023/5/9
 */
sealed class ExecuteResult<out R> {
    object Success : ExecuteResult<Nothing>()
    data class Error(val e: Exception) : ExecuteResult<Nothing>()
    data class SuccessResult<out T>(val data: T) : ExecuteResult<T>()
    data class Failed(val code: Int, val error: Exception) : ExecuteResult<Nothing>()
}


