// ITaskStatusService.aidl
package com.station.stationdownloader;
// Declare any non-default types here with import statements

interface ITaskStatusService {
    /**
     * Demonstrates some basic types that you can use as parameters
     * and return values in AIDL.
     */
   Map<String,String> getTaskStatus();
   String getTaskStatusByUrl(String url);
   String startTask(String url,String path,in int[] selectIndexes);
   String stopTask(String url);
   String startAllTasks();
   String stopAllTask();
   String getDownloadPath();
   String getTorrentList();
   String uploadTorrentNotify(String url);
   String initMagnetUri(String url,String torrentName);
   String dumpTorrentInfo(String torrentPath);
   String deleteTask(String url,boolean isDeleteFile);
   String getTaskList();
   String getDownloadingTasksStatus();
   String setConfig(long speedLimit,int maxThread,String downloadPath);
   String getConfigSet();


}