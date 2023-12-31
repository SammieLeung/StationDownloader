package com.station.stationdownloader.ui.contract

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.result.contract.ActivityResultContract

class GetContentActivityContract : ActivityResultContract<Unit?, Uri?>() {
    override fun createIntent(context: Context, input: Unit?): Intent {
        val intent = Intent(Intent.ACTION_GET_CONTENT)
        intent.addCategory(Intent.CATEGORY_OPENABLE)
        intent.type = "*/*"
        return intent
    }

    override fun parseResult(resultCode: Int, intent: Intent?): Uri? {
       if(resultCode== Activity.RESULT_OK&&intent!=null){
           return intent?.data
       }
        return null
    }
}