package com.station.pluginscenter.base

import android.os.Bundle
import androidx.databinding.ViewDataBinding
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.AndroidViewModel
import com.station.pluginscenter.util.tool.ViewDataBindingHelper
import com.station.stationkitkt.ViewModelHelper

/**
 * author: Sam Leung
 * date:  2022/12/20
 */
open class BaseActivity<VDB:ViewDataBinding>: FragmentActivity() {
    lateinit var mBinding: VDB
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mBinding= ViewDataBindingHelper.inflateVDB(baseContext,this::class) as VDB
        setContentView(mBinding.root)
        mBinding.lifecycleOwner = this
    }

}