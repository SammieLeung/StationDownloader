// ITaskStatusService.aidl
package com.station.stationdownloader;
// Declare any non-default types here with import statements
import com.station.stationdownloader.ITaskServiceCallback;
import com.station.stationdownloader.ITaskServiceListener;
interface ITaskStatusService {
    /**
     * Demonstrates some basic types that you can use as parameters
     * and return values in AIDL.
     */
   void subscribeMovie(String url,String path,in int[] selectIndexes,String movieId,String movieType,ITaskServiceCallback callback);
   void startTask(String url,String path,in int[] selectIndexes,ITaskServiceCallback callback);
   void stopTask(String url,ITaskServiceCallback callback);
   void startAllTasks(ITaskServiceCallback callback);
   void stopAllTask(ITaskServiceCallback callback);
   void getDownloadPath(ITaskServiceCallback callback);
   void getTorrentList(ITaskServiceCallback callback);
   void uploadTorrentNotify(String url,ITaskServiceCallback callback);
   void initMagnetUri(String url,String torrentName,ITaskServiceCallback callback);
   void dumpTorrentInfo(String torrentPath,ITaskServiceCallback callback);
   void deleteTask(String url,boolean isDeleteFile,ITaskServiceCallback callback);
   void getTaskList(ITaskServiceCallback callback);
   void getDownloadStatus(String url,ITaskServiceCallback callback);
   void setConfig(String speedLimit,String maxThread,String downloadPath,ITaskServiceCallback callback);
   void getConfigSet(ITaskServiceCallback callback);
   void addServiceListener(ITaskServiceListener listener);
   void removeServiceListener(ITaskServiceListener listener);
}
