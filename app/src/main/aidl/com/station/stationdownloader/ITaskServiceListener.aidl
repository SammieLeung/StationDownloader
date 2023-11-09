// ITaskServiceListener.aidl
package com.station.stationdownloader;

// Declare any non-default types here with import statements

interface ITaskServiceListener {
    /**
     * Demonstrates some basic types that you can use as parameters
     * and return values in AIDL.
     */
    String getTag();
    void notify(String command,String data,String expand);
    void failed(String command,String reason,int code);
}