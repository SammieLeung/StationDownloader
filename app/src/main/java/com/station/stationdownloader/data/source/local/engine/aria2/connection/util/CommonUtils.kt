package com.station.stationdownloader.data.source.local.engine.aria2.connection.util

import org.json.JSONArray

class CommonUtils {
    companion object {
        fun toJSONArray(keys: Collection<String?>, skipNulls: Boolean = false): JSONArray {
            val array = JSONArray()
            for (key in keys) {
                if (skipNulls && key == null) continue
                array.put(key)
            }
            return array
        }
    }
}