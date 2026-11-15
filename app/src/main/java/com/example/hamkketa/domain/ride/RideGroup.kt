package com.example.hamkketa.domain.ride

import kotlinx.coroutines.flow.StateFlow

data class RideMember(
    val id: String,
    val name: String,
    val phone: String = "",
    val deviceName: String = "",
    val recordingConsent: Boolean = false,
    val settlementConsent: Boolean = false
)

data class CostItem(val id: String, val name: String, val included: Boolean = true)

fun defaultCostItems() = listOf(
    CostItem("fuel", "유류비"),
    CostItem("toll", "통행료"),
    CostItem("parking", "주차비")
)

data class RideGroup(
    val id: String,
    val name: String,
    val members: List<RideMember>,
    val rule: SplitRule = SplitRule.Equal,
    val exemptIds: Set<String> = emptySet(),
    val costs: List<CostItem> = defaultCostItems()
) {
    fun findMember(memberId: String): RideMember? = members.firstOrNull { it.id == memberId }
}

interface RideGroupRepository {
    // Browsing a group never changes BLE membership: only a started session owns it.
    val activeGroup: StateFlow<RideGroup?>
}
