package com.station.stationdownloader

data class TaskId(
    val engine: DownloadEngine,
    val id: String,
) {

    fun isInvalid(): Boolean {
        return engine == DownloadEngine.INVALID_ENGINE || id == INVALID_ID
    }

    companion object {
        const val INVALID_ID = "0"

        @JvmStatic
        val INVALID_TASK_ID = TaskId(engine = DownloadEngine.INVALID_ENGINE, id = INVALID_ID)
    }
}