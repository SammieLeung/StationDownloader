// ITaskServiceCallback.aidl
package com.station.stationdownloader;

// Declare any non-default types here with import statements

interface ITaskServiceCallback {
      void onResult(String result);
      void onFailed(String reason, int code);
}