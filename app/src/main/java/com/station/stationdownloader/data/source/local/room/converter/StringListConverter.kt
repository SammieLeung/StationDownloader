package com.station.stationdownloader.data.source.local.room.converter

import androidx.room.TypeConverter
import com.station.stationkitkt.MoshiHelper

class StringListConverter {
    @TypeConverter
    fun stringToStringList(dataString: String?): List<String> {
        if (dataString?.isEmpty() == true)
            return emptyList()
        return dataString?.let {
            MoshiHelper.fromJson<List<String>>(it)
        } ?: emptyList()
    }

    @TypeConverter
    fun fromStringList(dataList: List<String>?): String {
        return dataList?.let { MoshiHelper.toJson(it) } ?: "[]"
    }

    @TypeConverter
    fun stringToIntList(dataString: String?): List<Int> {
        if (dataString?.isEmpty() == true)
            return emptyList()
        return dataString?.let {
            MoshiHelper.fromJson<List<Int>>(it)
        } ?: emptyList()
    }

    @TypeConverter
    fun fromIntList(dataList: List<Int>?): String {
        return dataList?.let { MoshiHelper.toJson(it) } ?: "[]"
    }
}