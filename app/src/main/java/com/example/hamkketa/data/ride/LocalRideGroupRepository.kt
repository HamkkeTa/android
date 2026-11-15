package com.example.hamkketa.data.ride

import android.content.Context
import com.example.hamkketa.domain.ride.CostItem
import com.example.hamkketa.domain.ride.RideGroup
import com.example.hamkketa.domain.ride.RideGroupRepository
import com.example.hamkketa.domain.ride.RideMember
import com.example.hamkketa.domain.ride.SplitRule
import com.example.hamkketa.domain.ride.defaultCostItems
import com.google.gson.Gson
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

fun exampleGroups(): List<RideGroup> {
    val members = listOf(RideMember("sample-yubin", "정유빈"), RideMember("sample-daeun", "김다은"))
    return listOf(
        RideGroup("commute", "출근 카풀", members),
        RideGroup("evening", "퇴근 카풀", members),
        RideGroup(
            "club",
            "스키, 보드 동호회 카풀",
            members + RideMember("sample-jimin", "반지민"),
            SplitRule.Passengers
        )
    )
}

data class CarpoolData(val groups: List<RideGroup> = exampleGroups(), val activeId: String = "")

@Singleton
class LocalRideGroupRepository @Inject constructor(@ApplicationContext context: Context) :
    RideGroupRepository {
    private val preferences = context.getSharedPreferences("carpool", Context.MODE_PRIVATE)
    private val gson = Gson()
    private val mutableData = MutableStateFlow(load())
    val data = mutableData.asStateFlow()
    private val mutableActiveGroup = MutableStateFlow<RideGroup?>(null)
    override val activeGroup: StateFlow<RideGroup?> = mutableActiveGroup.asStateFlow()

    fun activate(groupId: String?) {
        mutableActiveGroup.value = data.value.groups.firstOrNull { it.id == groupId }
    }

    private fun load(): CarpoolData = runCatching {
        val json = preferences.getString("data", null) ?: return@runCatching CarpoolData()
        val root = com.google.gson.JsonParser.parseString(json).asJsonObject
        val groups = root.getAsJsonArray("groups") ?: return@runCatching CarpoolData()
        groups.forEach { entry ->
            val group = entry.asJsonObject
            if (!group.has("costs")) group.add("costs", gson.toJsonTree(defaultCostItems()))
            if (!group.has("rule")) group.addProperty("rule", "Equal")
            if (!group.has("exemptIds")) group.add("exemptIds", com.google.gson.JsonArray())
            group.getAsJsonArray("members")?.forEach { item ->
                val member = item.asJsonObject
                if (!member.has("phone")) member.addProperty("phone", "")
                if (!member.has("deviceName")) member.addProperty("deviceName", "")
            }
        }
        gson.fromJson(root, CarpoolData::class.java).copy(activeId = "")
    }.getOrElse { CarpoolData() }

    private fun save(groups: List<RideGroup>) {
        val value = CarpoolData(groups)
        preferences.edit().putString("data", gson.toJson(value)).apply()
        mutableData.value = value
        mutableActiveGroup.value = groups.firstOrNull { it.id == mutableActiveGroup.value?.id }
    }

    fun createGroup(name: String, members: List<RideMember>): String? {
        if (name.isBlank()) return null
        val id = UUID.randomUUID().toString()
        save(data.value.groups + RideGroup(id, name.trim(), members))
        return id
    }

    fun addMember(groupId: String, member: RideMember) {
        save(
            data.value.groups.map {
                if (it.id == groupId) it.copy(members = it.members + member) else it
            }
        )
    }

    fun removeMember(groupId: String, memberId: String) {
        save(
            data.value.groups.map {
                if (it.id == groupId) {
                    it.copy(
                        members = it.members.filterNot { member ->
                            member.id == memberId
                        },
                        exemptIds =
                        it.exemptIds - memberId
                    )
                } else {
                    it
                }
            }
        )
    }

    fun saveRule(groupId: String, rule: SplitRule, exempt: Set<String>, costs: List<CostItem>) {
        save(
            data.value.groups.map {
                if (it.id == groupId) {
                    it.copy(
                        rule = rule,
                        exemptIds = exempt.intersect(
                            it.members.map { member ->
                                member.id
                            }.toSet()
                        ),
                        costs = costs
                    )
                } else {
                    it
                }
            }
        )
    }
}
