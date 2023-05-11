package com.station.stationkitkt

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStoreOwner
import java.lang.reflect.ParameterizedType
import kotlin.reflect.KClass
import kotlin.reflect.full.superclasses

/**
 * author: Sam Leung
 * date:  2022/12/21
 */
object ViewModelHelper {
    fun createViewModel(owner: ViewModelStoreOwner, kClass: KClass<*>): ViewModel? {
        val clazz = getViewModelClass(kClass);
        return if (clazz != null)
            ViewModelProvider(owner).get(clazz)
        else null
    }

    private fun getViewModelClass(kClass: KClass<*>): Class<ViewModel>? {
        val type = kClass.java.genericSuperclass ?: return null
        if (type is ParameterizedType) {
            type.actualTypeArguments.forEach {
                if (it is Class<*> && isSubClassOfViewModel(it))
                    return it as Class<ViewModel>
            }
        }
        for (subKClass in kClass.superclasses) {
            return getViewModelClass(subKClass)
        }
        return null
    }

    private fun isSubClassOfViewModel(clazz: Class<*>?): Boolean {
        if (clazz == null)
            return false
        return ViewModel::class.java.isAssignableFrom(clazz)
    }
}