package com.station.stationkitkt

import com.station.device.StationDeviceTool

object DeviceUtil {
    fun getMqttPublicKey(): String {
        return StationDeviceTool.getMqttPublicKey()
    }

    fun getDeviceSN():String{
        return StationDeviceTool.getDeviceSN()
    }
}