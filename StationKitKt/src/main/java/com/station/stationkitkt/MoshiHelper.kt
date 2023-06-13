package com.station.stationkitkt

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type

/**
 * author: Sam Leung
 * date:  2023/2/4
 */
object MoshiHelper {
    abstract class MoshiTypeReference<T> // 自定义的类，用来包装泛型

    lateinit var moshi:Moshi
    fun init(){
        moshi= Moshi.Builder()
//        .add(FastJsonAdapter())
            .addLast(KotlinJsonAdapterFactory()).build()
    }

    inline fun <reified T> toJson(src: T, indent: String = ""): String {

        try {

            val jsonAdapter = moshi.adapter<T>(getGenericType<T>())
            return jsonAdapter.indent(indent).toJson(src)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return ""

    }

    inline fun <reified T> fromJson(jsonStr: String): T? {
        try {
            val jsonAdapter = moshi.adapter<T>(getGenericType<T>())
            return jsonAdapter.fromJson(jsonStr)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }


    inline fun <reified T> getGenericType(): Type {
        val type =
            object :
                MoshiTypeReference<T>() {}::class.java
                .genericSuperclass
                .let { it as ParameterizedType }
                .actualTypeArguments
                .first()
        return type

    }

}
