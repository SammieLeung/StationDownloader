package com.station.stationdownloader.ui.fragment

import androidx.fragment.app.activityViewModels
import com.station.stationdownloader.databinding.DialogFragmentAddNewTaskBinding
import com.station.stationdownloader.ui.base.BaseDialogFragment
import com.station.stationdownloader.ui.viewmodel.MainViewModel

class TaskConfigDialogFragment: BaseDialogFragment<DialogFragmentAddNewTaskBinding>() {
    val mainVM: MainViewModel by activityViewModels<MainViewModel>()
}