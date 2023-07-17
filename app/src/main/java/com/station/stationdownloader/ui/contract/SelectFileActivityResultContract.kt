package com.station.stationdownloader.ui.contract

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.result.contract.ActivityResultContract

class SelectFileActivityResultContract(
    private val selectType: SelectType = SelectType.SELECT_TYPE_DEVICE,
    private val isSupportNetwork: Boolean = false,
    private val showConfirmDialog: Boolean = false
) : ActivityResultContract<Unit?, Uri?>() {
    private var pickerDialogTitle: String? = null

    fun setPickerDialogTitle(title: String) {
        pickerDialogTitle = title
    }

    override fun createIntent(context: Context, input: Unit?): Intent {
        val intent = Intent(ACTION_FILE_PICKER)
        intent.putExtra(EXTRA_SELECT_TYPE, selectType.ordinal)
        intent.putExtra(EXTRA_SUPPORT_NET, isSupportNetwork)
        intent.putExtra(
            EXTRA_TITLE,
            pickerDialogTitle
        )
        intent.putExtra(EXTRA_CONFIRM_DIALOG, showConfirmDialog)
        return intent
    }

    override fun parseResult(resultCode: Int, intent: Intent?): Uri? {
        return intent?.data
    }
}

const val ACTION_FILE_PICKER = "com.firefly.FILE_PICKER"
const val EXTRA_SELECT_TYPE = "selectType"
const val EXTRA_SUPPORT_NET = "supportNet"
const val EXTRA_TITLE = "title"
const val EXTRA_CONFIRM_DIALOG = "enableConfrimDialog" //

enum class SelectType(val type: Int) {
    SELECT_TYPE_FOLDER(0),
    SELECT_TYPE_FILE(1),
    SELECT_TYPE_DEVICE(2)
}