package com.station.stationdownloader.ui.fragment

import android.os.Bundle
import android.view.View
import androidx.fragment.app.activityViewModels
import com.station.stationdownloader.databinding.DialogFragmentAddUriBinding
import com.station.stationdownloader.databinding.DialogFragmentTaskConfigBinding
import com.station.stationdownloader.ui.base.BaseDialogFragment
import com.station.stationdownloader.ui.viewmodel.MainViewModel

class AddTaskDialogFragment: BaseDialogFragment<DialogFragmentTaskConfigBinding>() {
    val vm: MainViewModel by activityViewModels<MainViewModel>()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
    }
}