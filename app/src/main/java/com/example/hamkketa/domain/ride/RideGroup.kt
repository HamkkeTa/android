package com.example.hamkketa.domain.ride

import kotlinx.coroutines.flow.StateFlow

data class RideMember(val id: String, val name: String, val phone: String = "")

data class RideGroup(
    val id: String,
    val name: String,
    val members: List<RideMember>,
    val rule: SplitRule = SplitRule.Equal,
    val exemptIds: Set<String> = emptySet()
) {
    fun findMember(memberId: String): RideMember? = members.firstOrNull { it.id == memberId }
}

interface RideGroupRepository {
    // Members of this group are boarded automatically when they connect.
    val activeGroup: StateFlow<RideGroup?>
}
