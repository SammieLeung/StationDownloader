package com.station.stationkitkt

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.databinding.ViewDataBinding
import java.lang.reflect.Method
import java.lang.reflect.ParameterizedType
import kotlin.reflect.KClass
import kotlin.reflect.full.superclasses

/**
 * author: Sam Leung
 * date:  2022/12/30
 */
object ViewDataBindingHelper {

    /**
     * 获取指定的 [viewBindingClass] 实例
     * @param viewBindingClass Class<T> ViewDataBinding的子类
     * @param context Context 上下文
     * @param parent ViewGroup 父视图容器
     * @return T?
     */
    fun <T : ViewDataBinding> inflateVDB(
        viewBindingClass: Class<T>,
        context: Context,
        parent: ViewGroup,
    ): T? {
        try {
            val inflate: Method? = viewBindingClass?.getDeclaredMethod("inflate",
                LayoutInflater::class.java,
                ViewGroup::class.java,
                Boolean::class.java)
            return inflate?.invoke(null,
                LayoutInflater.from(context),
                parent,
                false) as? T
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }

    /**
     * 获取[kClass]的泛型参数中的ViewDataBinding子类实例
     * @param context Context
     * @param kClass KClass<*>
     * @return ViewDataBinding?
     */
    fun inflateVDB(
        context: Context,
        kClass: KClass<*>,
    ): ViewDataBinding? {
        try {
            val clazz = getViewDataBindingClass(kClass)
            val inflate: Method? = clazz?.getDeclaredMethod(
                "inflate",
                LayoutInflater::class.java,
            )
            return inflate?.invoke(null, LayoutInflater.from(context)) as? ViewDataBinding
        } catch (e: Exception) {

            e.printStackTrace()
        }
        return null
    }

    /**
     *  获取[kClass]的泛型参数中的ViewDataBinding子类实例
     * @param context Context
     * @param parent ViewGroup
     * @param kClass KClass<*>
     * @return ViewDataBinding?
     */
    fun inflateVDB(
        context: Context,
        parent: ViewGroup,
        kClass: KClass<*>,
    ): ViewDataBinding? {
        try {
            val clazz = getViewDataBindingClass(kClass)
            val inflate: Method? = clazz?.getDeclaredMethod("inflate",
                LayoutInflater::class.java,
                ViewGroup::class.java,
                Boolean::class.java)
            return inflate?.invoke(null,
                LayoutInflater.from(context),
                parent,
                false) as? ViewDataBinding
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }

    /**
     * 返回给定类及其父类中泛型参数中的首个ViewDatabinding子类
     * @param kClass KClass<*>
     * @return Class<ViewDataBinding>?
     */
    private fun getViewDataBindingClass(kClass: KClass<*>): Class<ViewDataBinding>? {
        val type = kClass.java.genericSuperclass ?: return null
        if (type is ParameterizedType) {
            type.actualTypeArguments.forEach {
                if (it is Class<*> && isSubClassOfViewDataBinding(it)) return it as Class<ViewDataBinding>
            }
        }
        for (subKClass in kClass.superclasses) {
            return getViewDataBindingClass(subKClass)
        }
        return null
    }

    /**
     * 判断一个类是否继承Viewdatabinding
     * @param clazz Class<*>?
     * @return Boolean
     */
    private fun isSubClassOfViewDataBinding(clazz: Class<*>?): Boolean {
        if (clazz == null) return false
        return ViewDataBinding::class.java.isAssignableFrom(clazz)
    }

}



