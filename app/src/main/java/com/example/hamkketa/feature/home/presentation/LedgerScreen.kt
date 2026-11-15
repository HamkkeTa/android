package com.example.hamkketa.feature.home.presentation

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.hamkketa.domain.ride.RideGroup

@Composable
internal fun LedgerScreen(
    groups: List<RideGroup>,
    selected: RideGroup?,
    trips: List<PreviewTrip>,
    storedIds: Set<String>,
    wide: Boolean,
    onSelectGroup: (String) -> Unit,
    onTrip: (String) -> Unit
) {
    var monthlyAuto by rememberSaveable(selected?.id) { mutableStateOf(true) }
    Row(
        Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        groups.forEach { group ->
            ChoiceChip(group.name, selected?.id == group.id, onClick = { onSelectGroup(group.id) })
        }
    }
    TwoColumns(wide, first = {
        Column(verticalArrangement = Arrangement.spacedBy(24.dp)) {
            Surface(
                color = Soft,
                border = BorderStroke(1.dp, Line),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(
                    Modifier.fillMaxWidth().padding(28.dp),
                    verticalArrangement = Arrangement.spacedBy(18.dp)
                ) {
                    Text("동승자 몫 합계", color = Ink, fontSize = 23.sp, fontWeight = FontWeight.Bold)
                    Text(
                        won(
                            trips.sumOf {
                                it.passengerWon
                            }
                        ),
                        color = Ink,
                        fontSize = 46.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "11월 누적   ·   운행 ${trips.size}회   ·   ${trips.sumOf {
                            it.distanceKm
                        }}km",
                        color = Muted
                    )
                }
            }
            MemberCard(
                PreviewYubin.copy(deviceName = "${trips.size}회 탑승"),
                amount = trips.sumOf {
                    it.yubinWon
                }
            )
            MemberCard(
                PreviewDaeun.copy(deviceName = "${trips.size}회 탑승"),
                index = 1,
                amount = trips.sumOf {
                    it.daeunWon
                }
            )
            Text("UI 확인용 예시 장부 · 실제 운행이나 청구 내역이 아닙니다.", color = Muted, fontSize = 13.sp)
        }
    }, second = {
        Panel {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Heading("운행 기록")
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(monthlyAuto, { monthlyAuto = it })
                    Text("월말 자동 발송 (예시)", color = Muted, fontSize = 14.sp)
                }
            }
            BoxWithConstraints(Modifier.fillMaxWidth()) {
                val tableWidth = maxOf(maxWidth, 650.dp)
                Box(Modifier.horizontalScroll(rememberScrollState())) {
                    Column(Modifier.width(tableWidth)) {
                        LedgerRow("날짜", "거리", "김다은", "정유빈", "상태", header = true)
                        HorizontalDivider(color = Outline)
                        trips.forEach { trip ->
                            Surface(onClick = { onTrip(trip.id) }, color = Color.White) {
                                LedgerRow(
                                    trip.shortDate,
                                    "${trip.distanceKm}km",
                                    won(trip.daeunWon),
                                    won(trip.yubinWon),
                                    if (trip.id in storedIds) "장부 저장됨" else "누적 중"
                                )
                            }
                            HorizontalDivider(color = Line)
                        }
                        LedgerRow(
                            "합계",
                            "${trips.sumOf {
                                it.distanceKm
                            }}km",
                            won(trips.sumOf { it.daeunWon }),
                            won(trips.sumOf { it.yubinWon }),
                            "",
                            header = true
                        )
                    }
                }
            }
            Text("행을 누르면 해당 운행의 구간과 비용을 확인할 수 있어요.", color = Muted, fontSize = 13.sp)
        }
    })
}

@Composable
private fun LedgerRow(
    date: String,
    distance: String,
    daeun: String,
    yubin: String,
    status: String,
    header: Boolean = false
) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = if (header) 22.dp else 24.dp, horizontal = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val color = if (header) Ink else Muted
        val weight = if (header) FontWeight.Bold else FontWeight.Normal
        Text(date, Modifier.weight(1.5f), color, fontSize = 15.sp, fontWeight = weight)
        Text(distance, Modifier.weight(0.75f), color, fontSize = 15.sp, fontWeight = weight)
        Text(daeun, Modifier.weight(1f), color, fontSize = 15.sp, fontWeight = weight)
        Text(yubin, Modifier.weight(1f), color, fontSize = 15.sp, fontWeight = weight)
        Box(Modifier.weight(1.15f)) {
            if (!header && status.isNotEmpty()) {
                Surface(
                    shape = CircleShape,
                    color = if (status ==
                        "누적 중"
                    ) {
                        Color(0xFFFFE0E0)
                    } else {
                        Green.copy(alpha = 0.1f)
                    }
                ) {
                    Text(
                        status,
                        color = if (status == "누적 중") Color(0xFFB94C4C) else Green,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                    )
                }
            } else {
                Text(status, color = color, fontSize = 15.sp, fontWeight = weight)
            }
        }
    }
}

@Composable
internal fun SummaryScreen(
    trip: PreviewTrip,
    wide: Boolean,
    stored: Boolean,
    onStore: () -> Unit,
    onShare: () -> Unit
) {
    TwoColumns(wide, first = {
        Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
            Heading("탑승 구간")
            RouteDiagram()
            Text(
                "예시 거리 ${trip.distanceKm}km · 실제 지도나 차량 주행 데이터가 아닙니다.",
                color = Muted,
                fontSize = 13.sp
            )
        }
    }, second = {
        Panel {
            Heading("1인당 정산 금액", "${trip.date} · UI 예시 금액")
            MemberCard(PreviewDriver, driver = true, amount = trip.driverWon)
            MemberCard(PreviewYubin, amount = trip.yubinWon)
            MemberCard(PreviewDaeun, index = 1, amount = trip.daeunWon)
            HorizontalDivider(color = Line)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("운행 비용 합계", color = Muted)
                Text(
                    won(trip.driverWon + trip.passengerWon),
                    color = Ink,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Text(
                "동승자 ${won(trip.passengerWon)} · 운전자 ${won(trip.driverWon)}",
                color = Muted,
                fontSize = 14.sp
            )
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                ActionButton(
                    if (stored) "장부에 모음" else "장부에 모으기",
                    Modifier.weight(1f),
                    enabled = !stored,
                    outlined = true,
                    onClick = onStore
                )
                ActionButton("카톡으로 내보내기", Modifier.weight(1f), onClick = onShare)
            }
            Text("내보내기는 UI 안내만 표시하며 실제 메시지를 발송하지 않습니다.", color = Muted, fontSize = 13.sp)
        }
    })
}

@Composable
private fun RouteDiagram() {
    Box(
        Modifier.fillMaxWidth().height(
            420.dp
        ).background(Soft, RoundedCornerShape(16.dp)).padding(34.dp)
    ) {
        Canvas(Modifier.matchParentSize()) {
            val start = Offset(size.width * 0.08f, size.height * 0.87f)
            val middle = Offset(size.width * 0.43f, size.height * 0.57f)
            val end = Offset(size.width * 0.92f, size.height * 0.13f)
            drawLine(Color(0xFF7296DC), start, middle, strokeWidth = 6.dp.toPx())
            drawLine(Color(0xFF3165C5), middle, end, strokeWidth = 6.dp.toPx())
            drawCircle(Ink, 12.dp.toPx(), start)
            drawCircle(Ink, 12.dp.toPx(), end)
            drawCircle(Ink, 13.dp.toPx(), middle)
            drawCircle(Color.White, 10.dp.toPx(), middle)
        }
        Text("출발", color = Ink, modifier = Modifier.align(Alignment.BottomStart))
        Text("정유빈 합류 (8km)", color = Muted, modifier = Modifier.align(Alignment.Center))
        Text("회사", color = Ink, modifier = Modifier.align(Alignment.TopEnd))
    }
}
