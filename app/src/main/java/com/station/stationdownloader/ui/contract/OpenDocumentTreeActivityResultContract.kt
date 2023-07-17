package com.station.stationdownloader.ui.contract

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.result.contract.ActivityResultContract

class OpenDocumentTreeActivityResultContract: ActivityResultContract<Unit?, Uri?>() {
    override fun createIntent(context: Context, input: Unit?): Intent {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE)
        intent.addCategory(Intent.CATEGORY_DEFAULT)
        return intent
    }

    override fun parseResult(resultCode: Int, intent: Intent?): Uri? {
       return intent?.data
    }
}