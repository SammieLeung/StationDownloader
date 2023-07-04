package com.station.stationdownloader.contants

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