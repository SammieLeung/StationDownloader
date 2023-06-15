package com.station.stationdownloader.ui.base

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.ViewDataBinding
import androidx.fragment.app.DialogFragment
import com.station.stationkitkt.ViewDataBindingHelper

open class BaseDialogFragment<VDB : ViewDataBinding> : DialogFragment() {
    lateinit var mBinding: VDB

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        mBinding = ViewDataBindingHelper.inflateVDB(requireContext(), this::class) as VDB
        mBinding.lifecycleOwner = this.viewLifecycleOwner
        return mBinding.root
    }

    override fun onStart() {
        super.onStart()
        dialog!!.window!!.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
    }

}