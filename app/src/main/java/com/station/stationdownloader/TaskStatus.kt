package com.station.stationdownloader

import android.os.Parcel
import android.os.Parcelable
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong

data class TaskStatus(
    val msgId:Long=msgIdGenerator.incrementAndGet(),
    val taskId: Long,
    val url: String,
    val speed: Long,
    val downloadSize: Long,
    val totalSize: Long,
    val status: Int
):Parcelable {
    constructor(parcel: Parcel) : this(
        parcel.readLong(),
        parcel.readLong(),
        parcel.readString()?:"",
        parcel.readLong(),
        parcel.readLong(),
        parcel.readLong(),
        parcel.readInt()
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeLong(msgId)
        parcel.writeLong(taskId)
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