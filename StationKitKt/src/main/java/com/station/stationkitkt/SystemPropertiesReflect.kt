package com.station.stationkitkt

import java.lang.reflect.Method

/**
 * author: Sam Leung
 * date:  2023/5/12
 */
class SystemPropertiesReflect {

    companion object {
        private val SystemProperties: Class<*>? by lazy {
            try {
                Class.forName("android.os.SystemProperties")
            } catch (e: Exception) {
                null
            }
        }

        private val _getBoolean: Method? by lazy {
            try {
                SystemProperties?.getDeclaredMethod(
                    "getBoolean",
                    String::class.java,
                    Boolean::class.java
                )
            } catch (e: NoSuchMethodException) {
                e.printStackTrace()
                null
            } catch (e: SecurityException) {
                e.printStackTrace()
                null
            }
        }

        private val _get: Method? by lazy {
            try {
                SystemProperties?.getDeclaredMethod(
                    "get",
                    String::class.java,
                    String::class.java
                )
            } catch (e: NoSuchMethodException) {
                e.printStackTrace()
                null
            } catch (e: SecurityException) {
                e.printStackTrace()
                null
            }
        }

        private val _set: Method? by lazy {
            try {
                SystemProperties?.getDeclaredMethod(
                    "set",
                    String::class.java,
                    String::class.java
                )
            } catch (e: NoSuchMethodException) {
                e.printStackTrace()
                null
            } catch (e: SecurityException) {
                e.printStackTrace()
                null
            }
        }

        @JvmStatic
        fun getBoolean(key: String, defaultValue: Boolean): Boolean {
            try {
                return _getBoolean?.invoke(SystemProperties, key, defaultValue) as Boolean
            } catch (e: Exception) {
                e.printStackTrace()
            }
            return defaultValue
        }

        fun get(key: String, defaultValue: String): String {
            try {
                return _get?.invoke(SystemProperties, key, defaultValue) as String
            } catch (e: Exception) {
                e.printStackTrace()
            }
            return defaultValue
        }

        fun set(key: String, defaultValue: String) {
            try {
                _set?.invoke(SystemProperties, key, defaultValue) as String
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}