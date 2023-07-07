package com.station.stationdownloader.data.source.local.room.converter

import androidx.room.TypeConverter

class StringListConverter {
    @TypeConverter
    fun stringToStringList(dataString: String?): List<String> {
        if (dataString?.isEmpty() == true)
            return emptyList()
        return dataString?.split(",") ?: emptyList()
    }

    @TypeConverter
    fun fromStringList(dataList: List<String>?): String {
        return dataList?.joinToString(",") ?: ""
    }

    @TypeConverter
    fun stringToIntList(dataString: String?): List<Int> {
        if (dataString?.isEmpty() == true)
            return emptyList()
        val list = dataString?.split(",")
        return if (list?.isEmpty() == true) {
            emptyList()
        } else if (list?.size == 1 && list[0].isEmpty()) {
            emptyList()
        } else {
            list?.map {
                it.toInt()
            } ?: emptyList()
        }
    }

    @TypeConverter
    fun fromIntList(dataList: List<Int>?): String {
        return dataList?.joinToString(",") ?: ""
    }
}