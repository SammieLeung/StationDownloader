package com.station.stationdownloader.data

import com.station.stationdownloader.data.source.local.engine.MultiNewTaskConfigModel
import com.station.stationdownloader.data.source.local.engine.NewTaskConfigModel

sealed class MultiTaskResult {
    data class Begin(val multiNewTaskConfigModel: MultiNewTaskConfigModel) : MultiTaskResult()
    data class InitializingTask(val url: String) : MultiTaskResult()
    data class Success(val taskConfigModel: NewTaskConfigModel) : MultiTaskResult()
    data class Failed(val url: String, val error: IResult.Error) : MultiTaskResult()
    object Finish : MultiTaskResult()
}
