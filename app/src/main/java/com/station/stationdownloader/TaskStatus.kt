package com.station.stationdownloader

import android.content.Context
import android.os.Parcel
import android.os.Parcelable
import org.json.JSONObject
import java.util.concurrent.atomic.AtomicLong

data class TaskStatus(
    // 用于区分消息的id
    val msgId: Long = msgIdGenerator.incrementAndGet(),
    val taskId: TaskId,
    val url: String,
    val speed: Long,
    val downloadSize: Long,
    val totalSize: Long,
    val status: Int,
    val errorCode: ErrorCode = ErrorCode.NoneErrorCode
) : Parcelable {
    constructor(taskId: TaskId) : this(
        msgId = msgIdGenerator.incrementAndGet(),
        taskId = taskId,
        url = "",
        speed = 0,
        downloadSize = 0,
        totalSize = 0,
        status = ITaskState.STOP.code
    )

    constructor(taskId: TaskId, url: String, status: Int) : this(
        msgId = msgIdGenerator.incrementAndGet(),
        taskId = taskId,
        url = url,
        speed = 0,
        downloadSize = 0,
        totalSize = 0,
        status = status
    )

    private constructor(jsonObject: JSONObject, url: String = "") : this(
        msgId = msgIdGenerator.incrementAndGet(),
        taskId = TaskId(
            DownloadEngine.ARIA2,
            jsonObject.getString("gid")
        ),
        url = url,
        speed = jsonObject.getString("downloadSpeed").toLong(),
        downloadSize = jsonObject.getString("completedLength").toLong(),
        totalSize = jsonObject.getString("totalLength").toLong(),
        status = when (jsonObject.getString("status")) {
            "active" -> ITaskState.RUNNING.code
            "waiting" -> ITaskState.STOP.code
            "paused" -> ITaskState.STOP.code
            "complete" -> ITaskState.DONE.code
            "error" -> ITaskState.ERROR.code
            else -> ITaskState.UNKNOWN.code
        },
        errorCode = if (jsonObject.has("errorCode"))
            ErrorCode.Aria2ErrorCode(jsonObject.getString("errorCode"))
        else ErrorCode.NoneErrorCode
    )

    constructor(parcel: Parcel) : this(
        parcel.readLong(),
        TaskId(
            DownloadEngine.valueOf(parcel.readString() ?: DownloadEngine.XL.name),
            parcel.readString() ?: ""
        ),
        parcel.readString() ?: "",
        parcel.readLong(),
        parcel.readLong(),
        parcel.readLong(),
        parcel.readInt(),
        ErrorCode.create(parcel.readInt(), parcel.readString())
    )


    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeLong(msgId)
        parcel.writeString(taskId.engine.name)
        parcel.writeString(taskId.id)
        parcel.writeString(url)
        parcel.writeLong(speed)
        parcel.writeLong(downloadSize)
        parcel.writeLong(totalSize)
        parcel.writeInt(status)
        parcel.writeInt(errorCode.type)
        parcel.writeString(errorCode.code)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<TaskStatus> {
        val msgIdGenerator = AtomicLong(0)
        override fun createFromParcel(parcel: Parcel): TaskStatus {
            return TaskStatus(parcel)
        }

        override fun newArray(size: Int): Array<TaskStatus?> {
            return arrayOfNulls(size)
        }

        fun createForAria2(jsonObject: JSONObject, url: String = ""): TaskStatus {
            return TaskStatus(jsonObject, url)
        }
    }
}

sealed class ErrorCode(val type: Int, val code: String? = null) {
    object NoneErrorCode : ErrorCode(0)
    data class DefaultErrorCode(val defaultCode: String) : ErrorCode(1, defaultCode)
    data class Aria2ErrorCode(val aria2Code: String) : ErrorCode(2, aria2Code)

    fun toErrorMessage(context: Context): String = when (this) {
        is NoneErrorCode -> ""
        is DefaultErrorCode -> context.getString(R.string.default_exit_status, defaultCode)
        is Aria2ErrorCode -> when (aria2Code) {
            "0" -> context.getString(R.string.aria2_exit_status_0)
            "1" -> context.getString(R.string.aria2_exit_status_1)
            "2" -> context.getString(R.string.aria2_exit_status_2)
            "3" -> context.getString(R.string.aria2_exit_status_3)
            "4" -> context.getString(R.string.aria2_exit_status_4)
            "5" -> context.getString(R.string.aria2_exit_status_5)
            "6" -> context.getString(R.string.aria2_exit_status_6)
            "7" -> context.getString(R.string.aria2_exit_status_7)
            "8" -> context.getString(R.string.aria2_exit_status_8)
            "9" -> context.getString(R.string.aria2_exit_status_9)
            "10" -> context.getString(R.string.aria2_exit_status_10)
            "11" -> context.getString(R.string.aria2_exit_status_11)
            "12" -> context.getString(R.string.aria2_exit_status_12)
            "13" -> context.getString(R.string.aria2_exit_status_13)
            "14" -> context.getString(R.string.aria2_exit_status_14)
            "15" -> context.getString(R.string.aria2_exit_status_15)
            "16" -> context.getString(R.string.aria2_exit_status_16)
            "17" -> context.getString(R.string.aria2_exit_status_17)
            "18" -> context.getString(R.string.aria2_exit_status_18)
            "19" -> context.getString(R.string.aria2_exit_status_19)
            "20" -> context.getString(R.string.aria2_exit_status_20)
            "21" -> context.getString(R.string.aria2_exit_status_21)
            "22" -> context.getString(R.string.aria2_exit_status_22)
            "23" -> context.getString(R.string.aria2_exit_status_23)
            "24" -> context.getString(R.string.aria2_exit_status_24)
            "25" -> context.getString(R.string.aria2_exit_status_25)
            "26" -> context.getString(R.string.aria2_exit_status_26)
            "27" -> context.getString(R.string.aria2_exit_status_27)
            "28" -> context.getString(R.string.aria2_exit_status_28)
            "29" -> context.getString(R.string.aria2_exit_status_29)
            "30" -> context.getString(R.string.aria2_exit_status_30)
            "31" -> context.getString(R.string.aria2_exit_status_31)
            "32" -> context.getString(R.string.aria2_exit_status_32)
            else -> {
                ""
            }
        }
    }

    companion object {
        @JvmStatic
        fun create(type: Int, code: String?): ErrorCode {
            return when (type) {
                0 -> NoneErrorCode
                1 -> DefaultErrorCode(code ?: "")
                2 -> Aria2ErrorCode(code ?: "")
                else -> NoneErrorCode
            }
        }
    }
}
