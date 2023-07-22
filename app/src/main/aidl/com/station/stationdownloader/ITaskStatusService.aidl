// ITaskStatusService.aidl
package com.station.stationdownloader;
import com.station.stationdownloader.TaskStatus;
// Declare any non-default types here with import statements

interface ITaskStatusService {
    /**
     * Demonstrates some basic types that you can use as parameters
     * and return values in AIDL.
     */
   Map<String,TaskStatus> getTaskStatus();
   TaskStatus getTaskStatusByUrl(String url);
}