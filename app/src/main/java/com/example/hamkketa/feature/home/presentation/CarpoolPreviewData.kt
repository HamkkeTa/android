package com.example.hamkketa.feature.home.presentation

import com.example.hamkketa.domain.ride.RideMember

/** Fixed display fixtures. These are never fed to the real settlement calculator. */
data class PreviewTrip(
    val id: String,
    val groupId: String,
    val groupName: String,
    val date: String,
    val shortDate: String,
    val distanceKm: Int = 20,
    val driverWon: Long = 1100,
    val daeunWon: Long = 1100,
    val yubinWon: Long = 550
) {
    val passengerWon: Long get() = daeunWon + yubinWon
}

fun previewTrips(groupId: String, groupName: String): List<PreviewTrip> = listOf(
    "12" to "목",
    "11" to "수",
    "10" to "화",
    "09" to "월",
    "06" to "금"
).map { (day, weekday) ->
    PreviewTrip(
        "$groupId-2026-11-$day",
        groupId,
        groupName,
        "2026.11.$day",
        "11.$day ($weekday) 출근"
    )
}

val PreviewDriver = RideMember("driver", "박대경")
val PreviewYubin = RideMember("preview-yubin", "정유빈")
val PreviewDaeun = RideMember("preview-daeun", "김다은")
