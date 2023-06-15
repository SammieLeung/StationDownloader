package com.station.stationdownloader.ui.base

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.ViewDataBinding
import androidx.fragment.app.Fragment
import com.station.stationkitkt.ViewDataBindingHelper

open class BaseFragment<VDB : ViewDataBinding> : Fragment() {
    lateinit var mBinding: VDB
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        mBinding = ViewDataBindingHelper.inflateVDB(requireContext(), this::class) as VDB
        mBinding.lifecycleOwner = viewLifecycleOwner
        return mBinding.root
    }
}