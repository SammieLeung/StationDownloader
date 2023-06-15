package com.station.stationdownloader.ui.fragment

import android.os.Bundle
import android.view.View
import com.station.stationdownloader.databinding.FragmentDownloadtaskBinding
import com.station.stationdownloader.navgator.Destination
import com.station.stationdownloader.ui.base.BaseFragment

class DownloadingTaskFragment : BaseFragment<FragmentDownloadtaskBinding>() {


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        mBinding.bindState()
    }

    fun FragmentDownloadtaskBinding.bindState() {
    }

    companion object {
        fun newInstance(destination: Destination): DownloadedTaskFragment {
            val args = Bundle()
            val fragment = DownloadedTaskFragment()
            fragment.arguments = args
            args.putInt("destination", destination.ordinal)
            return fragment
        }
    }
}
