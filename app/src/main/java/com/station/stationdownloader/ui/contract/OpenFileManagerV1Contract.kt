package com.station.stationdownloader.ui.contract

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContract

class OpenFileManagerV1Contract(
) : ActivityResultContract<Bundle, Uri?>() {

    override fun createIntent(context: Context, input: Bundle): Intent {
        val intent = Intent(ACTION_FILE_PICKER_V1_OPEN_FILE)
            .apply {
                putExtras(input)
            }
        return intent
    }

    override fun parseResult(resultCode: Int, intent: Intent?): Uri? {
        return intent?.data
    }

    companion object{
        const val ACTION_FILE_PICKER_V1_OPEN_FILE = "com.firefly.FILE_PICKER"
        const val EXTRA_SELECT_TYPE = "selectType"
        const val EXTRA_SUPPORT_NET = "supportNet"
        const val EXTRA_TITLE = "title"
        const val EXTRA_CONFIRM_DIALOG = "enableConfrimDialog" //
    }
    enum class SelectType(val type: Int) {
        SELECT_TYPE_FOLDER(0),
        SELECT_TYPE_FILE(1),
        SELECT_TYPE_DEVICE(2)
    }
}



