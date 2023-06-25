package com.station.stationdownloader.ui.fragment

import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import com.station.stationdownloader.databinding.DialogFragmentAddUriBinding
import com.station.stationdownloader.databinding.DialogFragmentTaskConfigBinding
import com.station.stationdownloader.ui.base.BaseDialogFragment
import com.station.stationdownloader.ui.viewmodel.MainViewModel
import com.station.stationdownloader.ui.viewmodel.TaskConfigViewModel

class TaskConfigDialogFragment: BaseDialogFragment<DialogFragmentTaskConfigBinding>() {
    val mainVM: MainViewModel by activityViewModels<MainViewModel>()
    val vm: TaskConfigViewModel by viewModels<TaskConfigViewModel>()
}