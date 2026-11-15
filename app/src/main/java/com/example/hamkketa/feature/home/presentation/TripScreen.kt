package com.example.hamkketa.feature.home.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.hamkketa.data.ride.TripRepository
import com.example.hamkketa.domain.ride.RideGroup
import com.example.hamkketa.domain.ride.TripCalculator
import com.example.hamkketa.domain.ride.activePassengerIds

@Composable
internal fun TripScreen(repository: TripRepository, groups: List<RideGroup>) {
    val store by repository.state.collectAsStateWithLifecycle()
    val message by repository.message.collectAsStateWithLifecycle()
    val km by repository.km.collectAsStateWithLifecycle()
    val doorStatus by repository.doorStatus.collectAsStateWithLifecycle()
    var efficiency by remember(store.settings) {
        mutableStateOf(store.settings.kmPerLiter.toString())
    }
    var price by remember(store.settings) { mutableStateOf(store.settings.wonPerLiter.toString()) }
    var distance by remember { mutableStateOf("") }
    var rssi by remember { mutableStateOf("-55") }
    var expense by remember { mutableStateOf("") }
    var error by remember { mutableStateOf("") }
    val current = store.current
    Panel {
        Heading("운행 설정 · 테스트", "휘발유 / 수동 연비·단가 / 좌석 조건 제외")
        Text(message)
        Text(if (store.settings.simulation) "도어·RSSI는 테스트 버튼으로 입력합니다." else doorStatus)
        if (error.isNotEmpty()) Text(error)
        Text(
            "거리 출처: ${if (store.settings.simulation) "에뮬레이터 수동 입력" else "Pleos Vehicle SDK"} · ${km ?: "미수신"} km"
        )
        if (current == null) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(efficiency, {
                    efficiency = it
                }, label = { Text("연비 km/L") }, modifier = Modifier.weight(1f))
                OutlinedTextField(price, {
                    price = it
                }, label = { Text("휘발유 원/L") }, modifier = Modifier.weight(1f))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                listOf(true to "에뮬레이터 수동", false to "차량 SDK").forEach { (manual, title) ->
                    ActionButton(title, outlined = store.settings.simulation != manual) {
                        error = runCatching {
                            repository.configure(
                                store.settings.copy(
                                    kmPerLiter = efficiency.toDouble(),
                                    wonPerLiter = price.toDouble(),
                                    simulation = manual
                                )
                            )
                            ""
                        }.getOrElse { "연비와 단가에 올바른 양수를 입력해 주세요." }
                    }
                }
            }
            Text("시작 전 설정을 저장하고 누적 거리를 입력한 뒤 그룹의 ‘자동 시작’을 눌러 주세요.")
        }
        if (store.settings.simulation) {
            OutlinedTextField(distance, {
                distance = it
            }, label = { Text("누적 주행거리 km (예: 1000 → 1010)") }, modifier = Modifier.fillMaxWidth())
            ActionButton("누적 거리 반영") {
                error =
                    if (distance.toDoubleOrNull()?.let { repository.acceptKm(it) } ==
                        true
                    ) {
                        ""
                    } else {
                        "올바른 누적 거리를 입력해 주세요."
                    }
            }
        }
        if (current != null) {
            Text(
                "${current.group.name} · ${current.endKm - current.startKm} km · 하차 유예 ${current.settings.graceMs / 1000}초"
            )
            if (store.settings.simulation) {
                ActionButton("도어 열림 → 닫힘 테스트") { repository.doorCycle() }
                OutlinedTextField(rssi, {
                    rssi = it
                }, label = { Text("테스트 RSSI dBm (기준 ${store.settings.rssiThreshold})") })
            }
            current.group.members.forEach { member ->
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "${member.name} · ${if (member.id in current.activePassengerIds()) "탑승 중 (하차 유예 포함)" else "미탑승"}"
                    )
                    SelectionContainer { Text("연결 ID: ${member.id}") }
                    if (store.settings.simulation) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            TextButton(onClick = {
                                rssi.toIntOrNull()?.let { repository.signal(member.id, it) }
                            }) { Text("연결 / 재연결") }
                            TextButton(onClick = {
                                repository.disconnected(member.id)
                            }) { Text("연결 해제") }
                        }
                    }
                }
            }
            Text("통행료·주차비 등은 API 연동 전 직접 입력하며, 현재 구간에 배분됩니다.")
            OutlinedTextField(expense, { expense = it }, label = { Text("추가 비용 원") })
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                current.group.costs.filter { it.included && it.id != "fuel" }.forEach { cost ->
                    TextButton(onClick = {
                        if (expense.toLongOrNull()?.let { repository.expense(cost.id, it) } ==
                            true
                        ) {
                            expense = ""
                        }
                    }) { Text("${cost.name} 추가") }
                }
            }
            ActualTrip(current)
        }
    }
    Heading("실제 운행 장부", "예시 기록과 별도로 저장됩니다. 금액은 10원 단위, 반올림 차액을 배분합니다.")
    if (store.records.isEmpty()) Text("종료한 운행이 없어요.")
    store.records.asReversed().forEach { record -> Panel { ActualTrip(record) } }
    if (current == null) {
        Heading("동승자 테스트 앱 연결 ID")
        groups.forEach { group ->
            SelectionContainer {
                Text(
                    "${group.name}\n" + group.members.joinToString("\n") { "${it.name}: ${it.id}" }
                )
            }
        }
    }
}

@Composable
internal fun ActualTrip(record: com.example.hamkketa.domain.ride.TripRecord) {
    val result = remember(record) { TripCalculator.calculate(record) }
    Text("${record.group.name} · ${if (record.endedAt == null) "계산 중" else "운행 종료"}")
    Text(
        "${record.settings.kmPerLiter} km/L · ${record.settings.wonPerLiter} 원/L · ${if (record.settings.simulation) "수동 테스트" else "차량 SDK"}"
    )
    if (record.interrupted) {
        Text("정산 보류 · 앱 중단 또는 거리 누락이 있습니다. 아래 금액은 마지막 저장 구간의 참고값입니다.")
    }
    Text("운전자: ${won(result.amounts["driver"] ?: 0)}")
    record.group.members.forEach { member ->
        Text(
            "${member.name}: ${won(result.amounts[member.id] ?: 0)} · ${"%.2f".format(
                result.distanceByMember[member.id] ?: 0.0
            )} km"
        )
    }
    Text(
        "합계 ${won(
            result.amounts.values.sum()
        )} · ${"%.2f".format(record.endKm - record.startKm)} km"
    )
    result.segments.forEachIndexed { index, segment ->
        Text(
            "구간 ${index + 1}: ${"%.2f".format(
                segment.distanceKm
            )} km · 동승자 ${segment.passengerIds.size}명 · 약 ${won(segment.costWon)}"
        )
    }
    Text("연료비 = 거리 ÷ 연비 × 단가. 미입력 추가 비용은 청구하지 않습니다.")
    if (record.endedAt != null && !record.interrupted) SettlementRequest(listOf(record), "이번 운행")
}

@Composable
internal fun ActualLedger(repository: TripRepository, groups: List<RideGroup>) {
    val store by repository.state.collectAsStateWithLifecycle()
    var selected by remember { mutableStateOf<String?>(null) }
    var detail by remember { mutableStateOf<String?>(null) }
    var period by remember { mutableStateOf("전체") }
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        listOf("전체", "최근 7일", "이번 달").forEach { title ->
            TextButton(onClick = {
                period = title
                detail = null
            }) { Text(title) }
        }
    }
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        TextButton(onClick = {
            selected = null
            detail = null
        }) { Text("전체 그룹") }
        groups.forEach { group ->
            TextButton(onClick = {
                selected = group.id
                detail = null
            }) { Text(group.name) }
        }
    }
    val now = java.time.ZonedDateTime.now()
    val since = when (period) {
        "최근 7일" -> now.minusDays(6).toLocalDate().atStartOfDay(now.zone).toInstant().toEpochMilli()
        "이번 달" -> now.withDayOfMonth(
            1
        ).toLocalDate().atStartOfDay(now.zone).toInstant().toEpochMilli()
        else -> 0L
    }
    val records = store.records.filter {
        (selected == null || it.group.id == selected) && it.startedAt >= since
    }.asReversed()
    SettlementRequest(records, period)
    if (records.isEmpty()) Text("저장된 운행이 없어요. 그룹에서 자동 시작 후 운행을 종료해 주세요.")
    records.forEach { record ->
        Panel {
            val date = java.text.SimpleDateFormat("yyyy.MM.dd HH:mm", java.util.Locale.KOREA)
                .format(java.util.Date(record.startedAt))
            TextButton(onClick = { detail = if (detail == record.id) null else record.id }) {
                Text(
                    "$date · ${record.group.name} · ${"%.2f".format(
                        record.endKm - record.startKm
                    )} km · ${if (record.interrupted) {
                        "정산 보류"
                    } else {
                        won(
                            TripCalculator.calculate(record).amounts.values.sum()
                        )
                    }} · 상세 보기"
                )
            }
            if (detail == record.id) ActualTrip(record)
        }
    }
}
