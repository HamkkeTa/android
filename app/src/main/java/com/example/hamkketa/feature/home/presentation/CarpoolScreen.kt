package com.example.hamkketa.feature.home.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.hamkketa.domain.ride.CostSegment
import com.example.hamkketa.domain.ride.RideMember
import com.example.hamkketa.domain.ride.SessionStatus
import com.example.hamkketa.domain.ride.SettlementCalculator
import com.example.hamkketa.domain.ride.SplitRule
import java.text.NumberFormat
import java.util.Locale

private val Ink = Color(0xFF163832)
private val Green = Color(0xFF087F68)
private val Muted = Color(0xFF64766F)
private val Paper = Color(0xFFF3F7F4)
private fun won(value: Long) = NumberFormat.getNumberInstance(Locale.KOREA).format(value) + "원"

@Composable
fun HomeRoute(modifier: Modifier = Modifier, viewModel: CarpoolViewModel = hiltViewModel()) {
    val data by viewModel.data.collectAsStateWithLifecycle()
    val boarding by viewModel.boarding.collectAsStateWithLifecycle()
    val parked by viewModel.parked.collectAsStateWithLifecycle()
    var page by rememberSaveable { mutableStateOf("그룹") }
    val group = data.groups.firstOrNull { it.id == data.activeId }
    val editable = parked && boarding.riders.isEmpty()
    var dialog by rememberSaveable { mutableStateOf("") }
    var groupName by rememberSaveable { mutableStateOf("") }
    var memberName by rememberSaveable { mutableStateOf("") }
    var phone by rememberSaveable { mutableStateOf("") }
    var consent by rememberSaveable { mutableStateOf(false) }
    var draftRule by remember(data.rule, page) { mutableStateOf(data.rule) }
    var exempt by remember(data.exemptIds, page) { mutableStateOf(data.exemptIds) }
    val examples = listOf(RideMember("sample-a", "정유빈"), RideMember("sample-b", "김다은"))
    val members = group?.members.orEmpty()
    val previewMembers = members.ifEmpty { examples }
    val segments = listOf(
        CostSegment(8.0, 1200, previewMembers.take(1).map { it.id }),
        CostSegment(12.0, 2100, previewMembers.map { it.id })
    )
    val amounts = SettlementCalculator.calculate(
        segments,
        if (page ==
            "분담 규칙"
        ) {
            draftRule
        } else {
            data.rule
        },
        exempt
    )

    BoxWithConstraints(modifier.fillMaxSize().background(Paper)) {
        val wide = maxWidth >= 840.dp
        Row(Modifier.fillMaxSize()) {
            Column(
                Modifier.width(
                    if (wide) 126.dp else 88.dp
                ).fillMaxHeight().background(Ink).padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    "함께타",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 22.sp,
                    modifier = Modifier.padding(vertical = 20.dp)
                )
                listOf("그룹", "장부", "운행 요약", "분담 규칙").forEach { tab ->
                    FilledTonalButton(
                        onClick = {
                            page = tab
                        },
                        modifier = Modifier.fillMaxWidth().heightIn(min = 60.dp),
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = if (page ==
                                tab
                            ) {
                                Color(0xFFD4F3DD)
                            } else {
                                Ink
                            },
                            contentColor = if (page ==
                                tab
                            ) {
                                Ink
                            } else {
                                Color.White
                            }
                        )
                    ) { Text(tab) }
                }
                Spacer(Modifier.weight(1f))
                Text("타고, 내리면\n함께 나눠요.", color = Color(0xFFB8D1C6), fontSize = 12.sp)
            }
            Column(
                Modifier.weight(
                    1f
                ).verticalScroll(rememberScrollState()).padding(if (wide) 28.dp else 16.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            "HAMKKETA  /  CARPOOL",
                            color = Green,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(page, fontSize = 30.sp, fontWeight = FontWeight.Bold, color = Ink)
                    }
                    Text("동승 ${boarding.riders.size}명", color = Green, fontWeight = FontWeight.Bold)
                }
                Panel {
                    Text(
                        when (boarding.status) {
                            SessionStatus.Open -> "● BLE 탑승 세션 열림"
                            SessionStatus.Closed -> "○ BLE 세션 대기"
                            SessionStatus.Unavailable -> "○ BLE 사용 불가 · 권한과 블루투스를 확인하세요"
                        },
                        color = Green
                    )
                    Text(
                        "차량 센서 미연동 · 자동 주행 잠금은 아직 지원하지 않습니다. 실제 주행 중에는 사용하지 마세요.",
                        color = Muted,
                        fontSize = 13.sp
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = parked, onCheckedChange = viewModel::confirmParked)
                        Text("정차 상태입니다 · 편집 허용", color = Ink)
                    }
                    if (boarding.riders.isNotEmpty()) {
                        Text(
                            "탑승 세션에 동승자가 있어 그룹과 규칙 편집이 잠겼어요.",
                            color = Muted
                        )
                    }
                }
                when (page) {
                    "그룹" -> {
                        Text(
                            "자주 함께하는 사람들",
                            color = Ink,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold
                        )
                        AdaptivePanels(wide, {
                            data.groups.forEach { item ->
                                OutlinedButton(
                                    onClick = {
                                        viewModel.select(item.id)
                                    },
                                    enabled = editable,
                                    modifier = Modifier.fillMaxWidth().heightIn(
                                        min = 80.dp
                                    ),
                                    shape = RoundedCornerShape(18.dp)
                                ) {
                                    Column(Modifier.fillMaxWidth()) {
                                        Text(
                                            (if (item.id == data.activeId) "●  " else "") +
                                                item.name,
                                            fontSize = 20.sp
                                        )
                                        Text("운전자 + 동승자 ${item.members.size}명", fontSize = 13.sp)
                                    }
                                }
                            }
                            Button(onClick = {
                                dialog = "group"
                            }, enabled = editable, modifier = Modifier.fillMaxWidth()) {
                                Text("+ 새 그룹 만들기")
                            }
                        }, {
                            Text(
                                group?.name.orEmpty(),
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold,
                                color = Ink
                            )
                            Text("운전자 · 나", color = Muted)
                            members.forEach { member ->
                                HorizontalDivider()
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Column(Modifier.weight(1f)) {
                                        Text(member.name, color = Ink, fontSize = 19.sp)
                                        Text(
                                            if (boarding.riders.any {
                                                    it.id == member.id
                                                }
                                            ) {
                                                "● 탑승 중"
                                            } else {
                                                "연결 대기"
                                            },
                                            color = Green
                                        )
                                        SelectionContainer {
                                            Text(
                                                "연결 ID: ${member.id}",
                                                color = Muted,
                                                fontSize = 11.sp
                                            )
                                        }
                                    }
                                    TextButton(onClick = {
                                        viewModel.removeMember(member.id)
                                    }, enabled = editable) { Text("삭제") }
                                }
                            }
                            if (members.isEmpty()) {
                                Text(
                                    "첫 동승자를 등록해 보세요.\n등록된 ID로 동승자 앱이 BLE에 접속합니다.",
                                    color = Muted
                                )
                            }
                            Button(onClick = {
                                dialog = "member"
                            }, enabled = editable, modifier = Modifier.fillMaxWidth()) {
                                Text("+ 동승자 등록")
                            }
                            Text(
                                "BLE GATT 연결을 사용합니다. 일반 오디오 블루투스 페어링과는 별개예요.",
                                color = Muted,
                                fontSize = 13.sp
                            )
                        })
                    }
                    "분담 규칙" -> AdaptivePanels(wide, {
                        Text("누가 비용을 나눌까요?", fontSize = 22.sp, fontWeight = FontWeight.Bold)
                        SplitRule.entries.forEach { rule ->
                            OutlinedButton(
                                onClick = {
                                    draftRule = rule
                                },
                                enabled = editable,
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(
                                    16.dp
                                )
                            ) {
                                Column(Modifier.fillMaxWidth().padding(8.dp)) {
                                    Text(
                                        (
                                            if (draftRule ==
                                                rule
                                            ) {
                                                "● "
                                            } else {
                                                "○ "
                                            }
                                            ) + rule.title,
                                        fontSize = 17.sp
                                    )
                                    Text(rule.description, fontSize = 13.sp)
                                }
                            }
                        }
                        if (draftRule == SplitRule.Exempt) {
                            previewMembers.forEach { member ->
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Checkbox(
                                        member.id in exempt,
                                        { checked ->
                                            exempt =
                                                if (checked) {
                                                    exempt + member.id
                                                } else {
                                                    exempt -
                                                        member.id
                                                }
                                        },
                                        enabled =
                                        editable && members.isNotEmpty()
                                    )
                                    Text(member.name + " 면제")
                                }
                            }
                        }
                        Text("실비만 분담 · 10원 단위 · 반올림 차액 자동 배분", color = Muted)
                        Button(onClick = {
                            viewModel.saveRule(draftRule, exempt)
                        }, enabled = editable, modifier = Modifier.fillMaxWidth()) {
                            Text(
                                if (draftRule ==
                                    data.rule &&
                                    exempt == data.exemptIds
                                ) {
                                    "저장된 규칙"
                                } else {
                                    "규칙 저장"
                                }
                            )
                        }
                    }, {
                        Amounts(amounts, previewMembers)
                    })
                    "운행 요약" -> {
                        Text(
                            "계산 미리보기 · 실제 운행 기록이 아닙니다",
                            color = Green,
                            fontWeight = FontWeight.Bold
                        )
                        AdaptivePanels(wide, {
                            Text("탑승 구간", fontSize = 22.sp, fontWeight = FontWeight.Bold)
                            Text("출발 → 중간 합류 → 도착", color = Green, fontSize = 20.sp)
                            segments.forEachIndexed { index, segment ->
                                HorizontalDivider()
                                Text(
                                    "구간 ${index + 1}  ·  ${segment.distanceKm} km",
                                    fontSize = 20.sp
                                )
                                Text(
                                    "운전자 + 동승 ${segment.passengerIds.size}명  /  ${won(
                                        segment.costWon
                                    )}",
                                    color = Muted
                                )
                            }
                            Text(
                                "예시 총 실비  ${won(3300)}",
                                color = Ink,
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text("실측 거리·연비·유가·통행료 연동 후 실제 내역을 제공합니다.", color = Muted)
                        }, { Amounts(amounts, previewMembers) })
                    }
                    "장부" -> Panel {
                        Text(
                            "정기 카풀 장부",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = Ink
                        )
                        Text("아직 저장된 운행이 없어요", fontSize = 22.sp, color = Ink)
                        Text(
                            "차량 주행거리와 구간 기록 연동 후 누적 내역을 보여드릴게요. 문자·알림톡 발송과 월말 청구는 서버 연동이 필요합니다.",
                            color = Muted
                        )
                        OutlinedButton(onClick = { page = "운행 요약" }) { Text("운행 요약 미리보기") }
                    }
                }
            }
        }
    }
    if (dialog.isNotEmpty()) {
        AlertDialog(
            onDismissRequest = { dialog = "" },
            title = { Text(if (dialog == "group") "새 그룹 만들기" else "동승자 등록") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    if (dialog ==
                        "group"
                    ) {
                        OutlinedTextField(groupName, {
                            groupName = it
                        }, label = { Text("그룹 이름") }, singleLine = true)
                    } else {
                        Text("동승자와 함께 확인해 주세요. 등록 후 표시되는 연결 ID를 동승자 앱에서 사용합니다.")
                        OutlinedTextField(memberName, {
                            memberName = it
                        }, label = { Text("이름") }, singleLine = true)
                        OutlinedTextField(phone, {
                            phone = it
                        }, label = { Text("전화번호") }, singleLine = true)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(consent, { consent = it })
                            Text("동승자가 이름·전화번호의 차량 내 저장에 동의했습니다.")
                        }
                        Text("자동 기기 검색·문자 발송은 아직 지원하지 않습니다.", color = Muted)
                    }
                }
            },
            confirmButton = {
                TextButton(
                    enabled =
                    editable &&
                        if (dialog ==
                            "group"
                        ) {
                            groupName.isNotBlank()
                        } else {
                            memberName.isNotBlank() &&
                                phone.filter(Char::isDigit).length in 10..11 &&
                                consent
                        },
                    onClick = {
                        if (dialog ==
                            "group"
                        ) {
                            viewModel.addGroup(groupName)
                        } else {
                            viewModel.addMember(memberName, phone)
                        }
                        dialog = ""
                        groupName = ""
                        memberName = ""
                        phone = ""
                        consent = false
                    }
                ) { Text("저장") }
            },
            dismissButton = { TextButton(onClick = { dialog = "" }) { Text("취소") } }
        )
    }
}

@Composable
private fun Panel(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Surface(modifier, shape = RoundedCornerShape(24.dp), color = Color.White) {
        Column(
            Modifier.padding(22.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            content = content
        )
    }
}

@Composable
private fun AdaptivePanels(
    wide: Boolean,
    first: @Composable ColumnScope.() -> Unit,
    second: @Composable ColumnScope.() -> Unit
) {
    if (wide) {
        Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
            Panel(Modifier.weight(1f), first)
            Panel(Modifier.weight(1.2f), second)
        }
    } else {
        Panel(Modifier.fillMaxWidth(), first)
        Panel(Modifier.fillMaxWidth(), second)
    }
}

@Composable
private fun Amounts(amounts: Map<String, Long>, members: List<RideMember>) {
    Text("이 규칙이면 이렇게 나눠요", color = Ink, fontSize = 22.sp, fontWeight = FontWeight.Bold)
    Text("예시 비용 3,300원 · 2개 구간", color = Muted)
    amounts.forEach { (id, amount) ->
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(
                if (id ==
                    SettlementCalculator.DRIVER
                ) {
                    "나 (운전자)"
                } else {
                    members.firstOrNull { it.id == id }?.name.orEmpty()
                },
                color = Ink
            )
            Text(won(amount), color = Green, fontWeight = FontWeight.Bold)
        }
        LinearProgressIndicator(progress = {
            amount / 3300f
        }, modifier = Modifier.fillMaxWidth().height(10.dp), color = Green, trackColor = Paper)
    }
    Text("합계 ${won(amounts.values.sum())}", fontWeight = FontWeight.Bold, color = Ink)
}
