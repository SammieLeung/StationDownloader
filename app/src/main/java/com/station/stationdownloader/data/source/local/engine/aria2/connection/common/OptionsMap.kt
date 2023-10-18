package com.station.stationdownloader.data.source.local.engine.aria2.connection.common

import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.io.Serializable

class OptionsMap : HashMap<String, OptionsMap.OptionValue>() {

    @Throws(JSONException::class)
    fun toJson(): JSONObject {
        if (isEmpty())
            return JSONObject()

        val json = JSONObject()
        for ((key, value) in entries) {
            json.put(key, value.formatString())
        }
        return json
    }

     class OptionValue(vararg var values: String) : Serializable {

        fun isEmpty(): Boolean {
            return values.isEmpty() || values[0].isEmpty()
        }

        fun formatString(separator: String? = null): Any {
            return if (values.isEmpty()) {
                ""
            } else if (values.size == 1) {
                values[0]
            } else if (separator != null) {
                values.joinToString(separator)
            } else {
                val array = JSONArray()
                for (value in values) array.put(value)
                array
            }
        }

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as OptionValue

            if (!values.contentEquals(other.values)) return false

            return true
        }

        override fun hashCode(): Int {
            return values.contentHashCode()
        }

    }
}