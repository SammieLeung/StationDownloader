package com.station.stationdownloader.ui.fragment

import androidx.fragment.app.activityViewModels
import com.station.stationdownloader.databinding.DialogFragmentAddSingleTaskBinding
import com.station.stationdownloader.ui.base.BaseDialogFragment
import com.station.stationdownloader.ui.viewmodel.MainViewModel

class TaskConfigDialogFragment: BaseDialogFragment<DialogFragmentAddSingleTaskBinding>() {
    val mainVM: MainViewModel by activityViewModels<MainViewModel>()
}