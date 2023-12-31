package com.station.stationdownloader.data

import com.station.stationdownloader.contants.UNKNOWN_ERROR

/**
 * author: Sam Leung
 * date:  2023/2/1
 */
sealed class IResult<out R> {
    data class Success<out T>(val data: T) : IResult<T>()
    data class Error( val exception: Exception,val code: Int = UNKNOWN_ERROR) : IResult<Nothing>()

    override fun toString(): String {
        return when (this) {
            is Success<*> -> "Success[data=$data]"
            is Error -> "Error[exception=$exception]"
        }
    }
}

val IResult<*>.succeeded
    get() = this is IResult.Success && data != null

val IResult<*>.isFailed
    get() = this is IResult.Error

fun<R> IResult<R>.result():R{
    return (this as IResult.Success<R>).data
}
