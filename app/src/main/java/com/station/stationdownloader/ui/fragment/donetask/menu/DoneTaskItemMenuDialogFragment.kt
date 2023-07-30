package com.station.stationdownloader.ui.fragment.downloading.menu

import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.lifecycle.lifecycleScope
import com.station.stationdownloader.databinding.DialogDoneTaskItemMenuBinding
import com.station.stationdownloader.ui.base.BaseDialogFragment
import com.station.stationdownloader.ui.contract.GetContentActivityContract
import com.station.stationdownloader.ui.fragment.donetask.DownloadedTaskFragment
import com.station.stationdownloader.ui.fragment.donetask.TaskMenuState
import com.station.stationdownloader.ui.fragment.donetask.UiAction
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch


@AndroidEntryPoint
class DoneTaskItemMenuDialogFragment : BaseDialogFragment<DialogDoneTaskItemMenuBinding>() {

    private val url: String by lazy {
        arguments?.getString(EXTRA_URL) ?: ""
    }

    private val vm by lazy {
        (requireParentFragment() as DownloadedTaskFragment).getViewModel()
    }

    private val openFileManager = registerForActivityResult(GetContentActivityContract()) {}

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        mBinding.bindState(
            vm.taskMenuState,
            vm.accept
        )
    }


    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        vm.accept(UiAction.HideTaskMenu)
    }

    private fun DialogDoneTaskItemMenuBinding.bindState(
        taskMenuState: StateFlow<TaskMenuState>,
        accept: (UiAction) -> Unit,
    ) {
        openFileBtn.setOnClickListener {
            accept(UiAction.GetFileUri(url))


        }
        deleteTaskBtn.setOnClickListener {

        }

        lifecycleScope.launch {
            taskMenuState.collect {
                if (it is TaskMenuState.FileUriState) {
                    val intent = Intent(Intent.ACTION_VIEW)
                    intent.data = it.fileUri
                    intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
            }
        }


    }

    companion object {
        const val EXTRA_URL = "url"
        fun newInstance(url: String): DoneTaskItemMenuDialogFragment {
            val args = Bundle()
            args.putString(EXTRA_URL, url)
            val fragment = DoneTaskItemMenuDialogFragment()
            fragment.arguments = args
            return fragment
        }
    }
}