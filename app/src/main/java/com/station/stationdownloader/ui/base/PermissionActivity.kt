package com.station.stationdownloader.ui.base

import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.databinding.ViewDataBinding

/**
 * author: Sam Leung
 * date:  2022/12/2
 */

open class PermissionActivity<VDB : ViewDataBinding>(val permissions: Array<String>) :
    BaseActivity<VDB>() {
    private val mRequestMultiplePermissionsLauncher by lazy {
        RequestMultiplePermissions {
            val grantedList = it.filterValues { it }.mapNotNull { it.key }
            val allGranted = grantedList.size == it.size

            if (allGranted) {
                Log.w("Welcome", "allGranted grantAllPermissions: ")
                grantAllPermissions()
            } else {
                var list = (it - grantedList).map { it.key }
                var deniedList =
                    list.filter { ActivityCompat.shouldShowRequestPermissionRationale(this, it) }
                val alwaysDeniedList = list - deniedList
                if (alwaysDeniedList.size > 0) {
                    deniedPermissions(alwaysDeniedList)
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (checkPermissions()) {
            grantAllPermissions()
        } else
            mRequestMultiplePermissionsLauncher.launch(permissions)
    }

    override fun onDestroy() {
        super.onDestroy()
        mRequestMultiplePermissionsLauncher.unregister()
    }


    private inline fun RequestMultiplePermissions(crossinline block: (Map<String, Boolean>) -> Unit): ActivityResultLauncher<Array<String>> {
        return registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { block(it) }
    }

    private fun checkPermissions(): Boolean {
        for (permission in permissions) {
            if (checkSelfPermission(permission) != PackageManager.PERMISSION_GRANTED)
                return false
        }
        return true
    }

    open fun grantAllPermissions() {
    }

    open fun deniedPermissions(map: List<String>) {

    }


}