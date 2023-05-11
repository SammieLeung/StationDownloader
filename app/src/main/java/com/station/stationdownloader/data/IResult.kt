package com.station.stationdownloader.data

/**
 * author: Sam Leung
 * date:  2023/2/1
 */
sealed class IResult<out R>{
    data class Success<out T>(val data:T):IResult<T>()
    data class Error(val exception: Exception):IResult<Nothing>()

    override fun toString(): String {
        return when (this) {
            is Success<*> -> "Success[data=$data]"
            is Error -> "Error[exception=$exception]"
        }
    }
}




val IResult<*>.succeeded
    get() = this is IResult.Success && data != null
