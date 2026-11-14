package com.example.hamkketa.domain.ride

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class BoardingStateTest {
    private val alice = RideMember(id = "a", name = "A")
    private val bob = RideMember(id = "b", name = "B")
    private val group = RideGroup(id = "g", name = "카풀", members = listOf(alice, bob))

    @Test
    fun onlyGroupMembersAreFound() {
        assertEquals(alice, group.findMember("a"))
        assertNull(group.findMember("stranger"))
    }

    @Test
    fun disconnectRemovesOnlyThatConnection() {
        val state =
            BoardingState()
                .boarded("device-1", alice)
                .boarded("device-2", bob)
                .alighted("device-1")

        assertEquals(listOf(bob), state.riders)
    }

    @Test
    fun sameMemberOnTwoDevicesCountsOnce() {
        val state = BoardingState().boarded("phone", alice).boarded("watch", alice)

        assertEquals(1, state.riders.size)
        assertEquals(1, state.alighted("phone").riders.size)
    }
}
