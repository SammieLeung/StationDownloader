package com.station.stationdownloader.ui.fragment.downloading

import android.os.Bundle
import android.view.View
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.station.stationdownloader.DownloadTaskStatus
import com.station.stationdownloader.data.source.local.room.entities.XLDownloadTaskEntity
import com.station.stationdownloader.databinding.FragmentDownloadtaskBinding
import com.station.stationdownloader.navgator.Destination
import com.station.stationdownloader.ui.base.BaseFragment
import com.station.stationdownloader.ui.fragment.DownloadedTaskFragment
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class DownloadingTaskFragment : BaseFragment<FragmentDownloadtaskBinding>() {
    val vm by viewModels<DownloadTaskManageViewModel>()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        mBinding.bindState(
            vm.downloadingTaskList
        )
    }

    private fun FragmentDownloadtaskBinding.bindState(downloadingTaskList: StateFlow<List<XLDownloadTaskEntity>>) {

        lifecycleScope.launch {
            downloadingTaskList.collectLatest {
                it.filter {
                    it.status==DownloadTaskStatus.DOWNLOADING
                }.forEach {

                }
            }
        }
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
