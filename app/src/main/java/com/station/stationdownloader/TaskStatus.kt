package com.station.stationdownloader

import android.os.Parcel
import android.os.Parcelable

data class TaskStatus(
    val taskId: Long,
    val url: String,
    val speed: Long,
    val downloadSize: Long,
    val totalSize: Long,
    val status: Int
):Parcelable {
    constructor(parcel: Parcel) : this(
        parcel.readLong(),
        parcel.readString()?:"",
        parcel.readLong(),
        parcel.readLong(),
        parcel.readLong(),
        parcel.readInt()
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
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
        override fun createFromParcel(parcel: Parcel): TaskStatus {
            return TaskStatus(parcel)
        }

        override fun newArray(size: Int): Array<TaskStatus?> {
            return arrayOfNulls(size)
        }
    }
}