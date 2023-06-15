package com.station.stationdownloader.ui.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.station.stationdownloader.databinding.FragmentDownloadtaskBinding
import com.station.stationdownloader.ui.base.BaseFragment

class DownloadedTaskFragment : BaseFragment<FragmentDownloadtaskBinding>() {


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        mBinding.text = 2.toString()
        super.onViewCreated(view, savedInstanceState)
    }
}