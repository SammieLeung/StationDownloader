package com.station.stationdownloader

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
    val status: Int
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

    constructor(taskId: TaskId, url: String) : this(
        msgId = msgIdGenerator.incrementAndGet(),
        taskId = taskId,
        url = url,
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
        parcel.readInt()
    )

    fun parseJSONObject(obj: JSONObject): TaskStatus {
        return copy(
            taskId = TaskId(
                DownloadEngine.ARIA2,
                obj.getString("gid")
            ),
            speed = obj.getString("downloadSpeed").toLong(),
            downloadSize = obj.getString("completedLength").toLong(),
            totalSize = obj.getString("totalLength").toLong(),
            status = when (obj.getString("status")) {
                "active" -> ITaskState.RUNNING.code
                "waiting" -> ITaskState.LOADING.code
                "paused" -> ITaskState.STOP.code
                "complete" -> ITaskState.DONE.code
                "error" -> ITaskState.ERROR.code
                else -> ITaskState.UNKNOWN.code
            }
        )
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeLong(msgId)
        parcel.writeString(taskId.engine.name)
        parcel.writeString(taskId.id)
        parcel.writeString(url)
        parcel.writeLong(speed)
        parcel.writeLong(downloadSize)
        parcel.writeLong(totalSize)
        parcel.writeInt(status)
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
    }
}