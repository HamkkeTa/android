package com.example.hamkketa.domain.ride

import kotlinx.coroutines.flow.StateFlow

enum class SessionStatus { Closed, Open, Unavailable }

data class BoardingState(
    val status: SessionStatus = SessionStatus.Closed,
    // Keyed by connection so a disconnect removes exactly that rider.
    val ridersByConnection: Map<String, RideMember> = emptyMap()
) {
    val riders: List<RideMember>
        get() = ridersByConnection.values.distinctBy { it.id }

    fun boarded(connectionId: String, member: RideMember): BoardingState =
        copy(ridersByConnection = ridersByConnection + (connectionId to member))

    fun alighted(connectionId: String): BoardingState = copy(
        ridersByConnection =
        ridersByConnection - connectionId
    )
}

interface BoardingSessionRepository {
    val state: StateFlow<BoardingState>

    fun open()

    fun close()
}
