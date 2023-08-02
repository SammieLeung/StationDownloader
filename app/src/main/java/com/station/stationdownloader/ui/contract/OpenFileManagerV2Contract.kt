package com.station.stationdownloader.ui.contract

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContract

class OpenFileManagerV2Contract : ActivityResultContract<Bundle, Intent?>() {
    override fun createIntent(context: Context, input: Bundle): Intent {
        val intent = Intent(ACTION_FILE_PICKER_V2_OPEN_FILE).apply {
            putExtras(input)
        }
        return intent
    }


    override fun parseResult(resultCode: Int, intent: Intent?): Intent? {
        return intent
    }

    companion object {
        const val ACTION_FILE_PICKER_V2_OPEN_FILE = "com.firefly.resourcemanager.SELECT"
        const val EXTRA_MIME_TYPE = "mime_type"
        const val EXTRA_SUFFIX_LIST="suffix_list"
    }
}



