package com.station.stationdownloader.data.source.remote.json
/*

        "download_size":524264103,
        "speed":553769,
        "status":1,
        "url":"thunder://QUFmdHA6Ly95Z2R5ODp5Z2R5OEB5ZzkwLmR5ZHl0dC5uZXQ6ODA4Mi8lRTklOTglQjMlRTUlODUlODklRTclOTQlQjUlRTUlQkQlQjF3d3cueWdkeTguY29tLiVFNiU5QyU4MCVFNyU4NyU4MyVFNyU5QSU4NCVFNiU4QiVCMyVFNSVBNCVCNC5CRC4xMDgwcC4lRTUlOUIlQkQlRTclQjIlQTQlRTUlOEYlOEMlRTglQUYlQUQlRTQlQjglQUQlRTUlQUQlOTcubWt2Wlo=",
        "is_done":false,
        "total_size":1590975833,
        "task_id":1001

 */
data class RemoteTaskStatus(
    val download_size: Long,
    val speed: Long,
    val status: Int,
    val url: String,
    val is_done: Boolean,
    val total_size: Long,
    val task_id: String
)