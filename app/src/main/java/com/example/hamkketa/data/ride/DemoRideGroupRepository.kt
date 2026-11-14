package com.example.hamkketa.data.ride

import com.example.hamkketa.domain.ride.RideGroup
import com.example.hamkketa.domain.ride.RideGroupRepository
import com.example.hamkketa.domain.ride.RideMember
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

// shortcut: 고정 데모 그룹, D-01 그룹 관리 구현 시 저장소 기반 구현으로 교체
@Singleton
class DemoRideGroupRepository @Inject constructor() : RideGroupRepository {
    override val activeGroup: StateFlow<RideGroup?> =
        MutableStateFlow(
            RideGroup(
                id = "demo-commute",
                name = "출근 카풀",
                members =
                listOf(
                    RideMember(id = "member-1", name = "동승자 1"),
                    RideMember(id = "member-2", name = "동승자 2"),
                    RideMember(id = "member-3", name = "동승자 3")
                )
            )
        )
}
