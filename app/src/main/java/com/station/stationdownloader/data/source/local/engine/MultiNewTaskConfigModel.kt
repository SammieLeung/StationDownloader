package com.station.stationdownloader.data.source.local.engine

import com.station.stationdownloader.DownloadEngine
import com.station.stationdownloader.FileType
import com.station.stationdownloader.data.IResult
import com.station.stationdownloader.data.source.local.model.TreeNode
import com.station.stationdownloader.data.source.local.model.filterFile

data class MultiNewTaskConfigModel(
    val downloadPath: String,
    val engine: DownloadEngine,
    val failedLinks: MutableList<Pair<String,IResult.Error>> = mutableListOf(),
    val linkCount: Int = 0,
    val taskConfigs: MutableList<NewTaskConfigModel> = mutableListOf()
) {
    fun addTask(taskConfig: NewTaskConfigModel):MultiNewTaskConfigModel {
        taskConfigs.add(taskConfig)
        return this
    }

    fun addFailedLink(link: String,error:IResult.Error):MultiNewTaskConfigModel {
        failedLinks.add(Pair(link,error))
        return this
    }

    fun filterFile(fileType: FileType, isChecked: Boolean) {
        taskConfigs.forEach {
            if (it._fileTree is TreeNode.Directory) {
                it._fileTree.filterFile(fileType, isChecked)
            }
        }
    }

    fun update(
        downloadPath: String = this.downloadPath,
        downloadEngine: DownloadEngine = this.engine,
    ): MultiNewTaskConfigModel {

        taskConfigs.forEachIndexed { index, newTaskConfigModel ->
            taskConfigs.set(
                index,
                newTaskConfigModel.update(
                    downloadPath = downloadPath,
                    downloadEngine = downloadEngine
                )
            )
        }
        return MultiNewTaskConfigModel(
            downloadPath = downloadPath,
            engine = downloadEngine,
            failedLinks = failedLinks,
            linkCount = linkCount,
            taskConfigs = taskConfigs
        )
    }
}
