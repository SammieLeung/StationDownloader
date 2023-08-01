package com.station.stationdownloader.ui.fragment.donetask.menu

import android.content.DialogInterface
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.DocumentsContract
import android.view.View
import androidx.lifecycle.lifecycleScope
import com.station.stationdownloader.databinding.DialogDoneTaskItemMenuBinding
import com.station.stationdownloader.ui.base.BaseDialogFragment
import com.station.stationdownloader.ui.fragment.donetask.DownloadedTaskFragment
import com.station.stationdownloader.ui.fragment.donetask.MenuDialogUiState
import com.station.stationdownloader.ui.fragment.donetask.UiAction
import com.station.stationdownloader.ui.fragment.donetask.UiState
import com.station.stationdownloader.utils.DLogger
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch


@AndroidEntryPoint
class DoneTaskItemMenuDialogFragment : BaseDialogFragment<DialogDoneTaskItemMenuBinding>(),
    DLogger {

    private val url: String by lazy {
        arguments?.getString(EXTRA_URL) ?: ""
    }

    private val vm by lazy {
        (requireParentFragment() as DownloadedTaskFragment).getViewModel()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        mBinding.bindState(
            vm.menuDialogUiState,
            vm.uiState,
            vm.accept
        )
    }

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        vm.accept(UiAction.ShowTaskMenu(isShow = false))
        vm.accept(UiAction.InitUiState)
    }

    private fun DialogDoneTaskItemMenuBinding.bindState(
        menuState: StateFlow<MenuDialogUiState>,
        uiState: StateFlow<UiState>,
        accept: (UiAction) -> Unit,
    ) {
        openFileBtn.setOnClickListener {
            accept(UiAction.OpenFile(url))
        }
        deleteTaskBtn.setOnClickListener {
            showConfirmDeleteDialog()
        }

        lifecycleScope.launch {
            menuState.collect {
                if (it.isDelete) {
                    dismiss()
                }
                if (!it.isShow) {
                    dismiss()
                }
            }
        }

        lifecycleScope.launch {
            uiState.collect {
                if (it is UiState.OpenFileState) {
                    openFile(it.uri)
                }
            }
        }

    }

    private fun openFile(uri: Uri) {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "*/*"

            // Optionally, specify a URI for the file that should appear in the
            // system file picker when it loads.
            putExtra(DocumentsContract.EXTRA_INITIAL_URI, uri)
        }
        if (intent.resolveActivity(requireActivity().packageManager) != null) {
            startActivity(intent);
        } else {
            logger("处理没有文件管理器应用的情况")
        }
    }

    private fun showConfirmDeleteDialog() {
        parentFragmentManager.beginTransaction()
            .add(ConfirmDeleteDialogFragment.newInstance(url), "confirm_delete")
            .hide(this)
            .addToBackStack("confirm_delete")
            .commit()
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

    override fun DLogger.tag(): String {
        return DoneTaskItemMenuDialogFragment.javaClass.simpleName
    }
}