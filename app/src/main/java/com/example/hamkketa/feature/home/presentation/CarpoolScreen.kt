package com.example.hamkketa.feature.home.presentation

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.hamkketa.domain.ride.CostSegment
import com.example.hamkketa.domain.ride.RideGroup
import com.example.hamkketa.domain.ride.RideMember
import com.example.hamkketa.domain.ride.SessionStatus
import com.example.hamkketa.domain.ride.SettlementCalculator
import com.example.hamkketa.domain.ride.SplitRule
import java.text.NumberFormat
import java.util.Locale

private val Ink = Color(0xFF3C3C3B)
private val Muted = Color(0xFF808080)
private val Soft = Color(0xFFF5F5F5)
private val Line = Color(0xFFDEDEDE)
private val SelectedLine = Color(0xFFB9B9B9)
private val DriverGreen = Color(0xFF529C7A)
private val AvatarColors = listOf(Color(0xFF6153B5), Color(0xFFC5802A), Color(0xFF5B88AE))
private fun won(value: Long) = NumberFormat.getNumberInstance(Locale.KOREA).format(value) + "원"

@Composable
fun HomeRoute(modifier: Modifier = Modifier, viewModel: CarpoolViewModel = hiltViewModel()) {
    val data by viewModel.data.collectAsStateWithLifecycle()
    val boarding by viewModel.boarding.collectAsStateWithLifecycle()
    val parked by viewModel.parked.collectAsStateWithLifecycle()
    var page by rememberSaveable { mutableStateOf("그룹") }
    var dialog by rememberSaveable { mutableStateOf("") }
    var showConnectionIds by rememberSaveable { mutableStateOf(false) }
    val group = data.groups.firstOrNull { it.id == data.activeId }
    val editable = parked && boarding.riders.isEmpty()
    var draftRule by remember(data.activeId, data.rule, page) { mutableStateOf(data.rule) }
    var exempt by remember(data.activeId, data.exemptIds, page) { mutableStateOf(data.exemptIds) }
    val members = group?.members.orEmpty()
    val previewMembers = members.ifEmpty {
        listOf(RideMember("sample-a", "정유빈"), RideMember("sample-b", "김다은"))
    }
    val segments = listOf(
        CostSegment(8.0, 1200, previewMembers.take(1).map { it.id }),
        CostSegment(12.0, 2100, previewMembers.map { it.id })
    )
    val amounts = SettlementCalculator.calculate(
        segments,
        if (page == "분담 규칙") draftRule else data.rule,
        if (page == "분담 규칙") exempt else data.exemptIds
    )

    BoxWithConstraints(modifier.fillMaxSize().background(Color.White)) {
        val wide = maxWidth >= 840.dp
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState())
                .padding(horizontal = if (wide) 48.dp else 24.dp, vertical = 32.dp),
            verticalArrangement = Arrangement.spacedBy(28.dp)
        ) {
            AppHeader(page, wide, onPage = { page = it })
            when (page) {
                "그룹" -> TwoColumns(
                    wide = wide,
                    firstWeight = 0.42f,
                    first = {
                        Column(
                            modifier = Modifier.padding(top = if (wide) 112.dp else 0.dp),
                            verticalArrangement = Arrangement.spacedBy(18.dp)
                        ) {
                            data.groups.forEach { item ->
                                GroupCard(
                                    group = item,
                                    selected = item.id == data.activeId,
                                    enabled = editable,
                                    onSelect = { viewModel.select(item.id) }
                                )
                            }
                            OutlinedButton(
                                onClick = { dialog = "group" },
                                enabled = editable,
                                modifier = Modifier.fillMaxWidth().heightIn(min = 68.dp),
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.5.dp, SelectedLine),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Muted)
                            ) {
                                Text("＋  새 그룹 만들기", fontSize = 22.sp)
                            }
                        }
                    },
                    second = {
                        MemberPanel(
                            group = group,
                            riders = boarding.riders.map { it.id }.toSet(),
                            editable = editable,
                            showConnectionIds = showConnectionIds,
                            onToggleIds = { showConnectionIds = !showConnectionIds },
                            onRegister = { dialog = "member" },
                            onRemove = viewModel::removeMember
                        )
                    }
                )
                "분담 규칙" -> TwoColumns(wide, first = {
                    Panel {
                        SectionTitle("분담 규칙", group?.name.orEmpty())
                        SplitRule.entries.forEach { rule ->
                            RuleCard(
                                rule = rule,
                                selected = draftRule == rule,
                                enabled = editable,
                                onSelect = { draftRule = rule }
                            )
                        }
                        if (draftRule == SplitRule.Exempt) {
                            members.forEach { member ->
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Checkbox(
                                        checked = member.id in exempt,
                                        onCheckedChange = { checked ->
                                            exempt =
                                                if (checked) {
                                                    exempt + member.id
                                                } else {
                                                    exempt -
                                                        member.id
                                                }
                                        },
                                        enabled = editable
                                    )
                                    Text("${member.name} 면제", color = Ink)
                                }
                            }
                            if (members.isEmpty()) Text("면제할 동승자를 먼저 등록해 주세요.", color = Muted)
                        }
                        Text("실비만 분담 · 10원 단위 · 반올림 차액 자동 배분", color = Muted, fontSize = 14.sp)
                        PrimaryButton(
                            text = if (draftRule == data.rule && exempt == data.exemptIds) {
                                "저장된 규칙"
                            } else {
                                "규칙 저장"
                            },
                            enabled = editable,
                            onClick = { viewModel.saveRule(draftRule, exempt) }
                        )
                    }
                }, second = {
                    Panel { Amounts(amounts, previewMembers) }
                })
                "운행 요약" -> TwoColumns(wide, first = {
                    Panel {
                        SectionTitle("탑승 구간", "계산 미리보기 · 실제 운행 기록이 아닙니다")
                        RoutePreview()
                        segments.forEachIndexed { index, segment ->
                            HorizontalDivider(color = Line)
                            Row(
                                Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text(
                                        "구간 ${index + 1}  ·  ${segment.distanceKm} km",
                                        color = Ink,
                                        fontSize = 20.sp
                                    )
                                    Text("운전자 + 동승자 ${segment.passengerIds.size}명", color = Muted)
                                }
                                Text(won(segment.costWon), color = Ink, fontSize = 20.sp)
                            }
                        }
                        HorizontalDivider(color = Line)
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("예시 총 실비", color = Muted)
                            Text(
                                won(3300),
                                color = Ink,
                                fontSize = 28.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Text("실측 거리·연비·유가·통행료 연동 후 실제 내역을 제공합니다.", color = Muted, fontSize = 14.sp)
                    }
                }, second = {
                    Panel { Amounts(amounts, previewMembers) }
                })
                "장부" -> Panel {
                    SectionTitle("정기 카풀 장부", group?.name.orEmpty())
                    Column(
                        Modifier.fillMaxWidth().padding(vertical = 48.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Box(
                            Modifier.size(80.dp).background(Soft, CircleShape),
                            contentAlignment = Alignment.Center
                        ) { Text("≡", fontSize = 42.sp, color = Muted) }
                        Text(
                            "아직 저장된 운행이 없어요",
                            color = Ink,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text("운행 기록이 쌓이면 이곳에서 함께 확인해요.", color = Muted)
                        OutlinedButton(onClick = { page = "운행 요약" }) { Text("운행 요약 미리보기") }
                    }
                    HorizontalDivider(color = Line)
                    Text("실제 장부 누적과 월말 청구는 차량 기록·서버 연동 후 제공됩니다.", color = Muted, fontSize = 14.sp)
                }
            }
            SessionFooter(
                status = boarding.status,
                riderCount = boarding.riders.size,
                parked = parked,
                onParkedChange = viewModel::confirmParked
            )
        }
    }
    if (dialog.isNotEmpty()) {
        RegistrationDialog(
            member = dialog == "member",
            editable = editable,
            onDismiss = { dialog = "" },
            onSave = { name, phone ->
                if (dialog ==
                    "group"
                ) {
                    viewModel.addGroup(name)
                } else {
                    viewModel.addMember(name, phone)
                }
                dialog = ""
            }
        )
    }
}

@Composable
private fun AppHeader(page: String, wide: Boolean, onPage: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(if (wide) 28.dp else 12.dp)
        ) {
            Text(
                "함께타",
                color = Ink,
                fontSize = if (wide) 76.sp else 34.sp,
                fontWeight = FontWeight.ExtraBold
            )
            Row(horizontalArrangement = Arrangement.spacedBy(if (wide) 16.dp else 8.dp)) {
                listOf("그룹", "장부").forEach { tab ->
                    FilterChip(
                        selected = page == tab,
                        onClick = { onPage(tab) },
                        label = {
                            Text(
                                tab,
                                fontSize = if (wide) 24.sp else 18.sp,
                                modifier = Modifier.padding(
                                    horizontal = if (wide) 8.dp else 4.dp,
                                    vertical = 4.dp
                                )
                            )
                        },
                        shape = CircleShape,
                        colors = FilterChipDefaults.filterChipColors(
                            containerColor = Color.White,
                            labelColor = Ink,
                            selectedContainerColor = Ink,
                            selectedLabelColor = Color.White
                        ),
                        border = BorderStroke(1.dp, if (page == tab) Ink else Line)
                    )
                }
            }
            if (wide) {
                Spacer(Modifier.weight(1f))
                DetailNavigation(page, onPage)
            }
        }
        if (!wide) DetailNavigation(page, onPage)
    }
}

@Composable
private fun DetailNavigation(page: String, onPage: (String) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        listOf("운행 요약", "분담 규칙").forEach { tab ->
            TextButton(onClick = { onPage(tab) }) {
                Text(
                    tab,
                    color = if (page ==
                        tab
                    ) {
                        Ink
                    } else {
                        Muted
                    },
                    fontWeight = if (page ==
                        tab
                    ) {
                        FontWeight.Bold
                    } else {
                        FontWeight.Normal
                    }
                )
            }
        }
    }
}

@Composable
private fun GroupCard(group: RideGroup, selected: Boolean, enabled: Boolean, onSelect: () -> Unit) {
    Surface(
        onClick = onSelect,
        enabled = enabled,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = if (selected) Color.White else Soft,
        contentColor = Ink,
        border = BorderStroke(if (selected) 2.dp else 1.dp, if (selected) SelectedLine else Line),
        shadowElevation = if (selected) 4.dp else 0.dp
    ) {
        Row(
            Modifier.padding(28.dp).heightIn(min = 78.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(group.name, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                Text(
                    "멤버 ${group.members.size + 1}명 · ${group.rule.shortTitle()}",
                    color = Muted,
                    fontSize = 16.sp
                )
            }
            if (selected) {
                Surface(color = Color(0xFF555554), shape = RoundedCornerShape(12.dp)) {
                    Text(
                        "선택됨",
                        color = Color.White,
                        fontSize = 16.sp,
                        modifier = Modifier.padding(horizontal = 15.dp, vertical = 14.dp)
                    )
                }
            }
        }
    }
}

private fun SplitRule.shortTitle() = when (this) {
    SplitRule.Equal -> "운전자 포함 균등"
    SplitRule.Passengers -> "동승자끼리만"
    SplitRule.Exempt -> "특정 동승자 면제"
}

@Composable
private fun MemberPanel(
    group: RideGroup?,
    riders: Set<String>,
    editable: Boolean,
    showConnectionIds: Boolean,
    onToggleIds: () -> Unit,
    onRegister: () -> Unit,
    onRemove: (String) -> Unit
) {
    Panel {
        SectionTitle(group?.name ?: "그룹을 선택하세요", "멤버 구성")
        Spacer(Modifier.height(8.dp))
        MemberCard("나", "운전자", DriverGreen, driver = true)
        group?.members.orEmpty().forEachIndexed { index, member ->
            MemberCard(
                name = member.name,
                subtitle = if (member.id in riders) "탑승 중" else "연결 대기",
                avatarColor = AvatarColors[index % AvatarColors.size],
                enabled = editable,
                connectionId = if (showConnectionIds) member.id else null,
                onRemove = { onRemove(member.id) }
            )
        }
        if (group?.members.isNullOrEmpty()) {
            Text(
                "첫 동승자를 등록해 함께 시작해 보세요.",
                color = Muted,
                modifier = Modifier.padding(vertical = 12.dp)
            )
        }
        Spacer(Modifier.height(10.dp))
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            color = Soft,
            border = BorderStroke(1.dp, Line)
        ) {
            BoxWithConstraints {
                val inline = maxWidth >= 520.dp
                Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        BluetoothAvatar()
                        Text(
                            "동승자 등록",
                            color = Muted,
                            fontSize = 21.sp,
                            modifier = Modifier.weight(1f)
                        )
                        if (inline) RegisterButton(editable, onRegister)
                    }
                    if (!inline) {
                        Box(Modifier.align(Alignment.End)) { RegisterButton(editable, onRegister) }
                    }
                    Text("등록한 동승자의 함께타 앱으로 BLE 연결을 진행해요.", color = Muted, fontSize = 13.sp)
                }
            }
        }
        if (!group?.members.isNullOrEmpty()) {
            TextButton(onClick = onToggleIds, modifier = Modifier.align(Alignment.End)) {
                Text(if (showConnectionIds) "연결 ID 숨기기" else "연결 ID 확인", color = Muted)
            }
        }
    }
}

@Composable
private fun RegisterButton(enabled: Boolean, onClick: () -> Unit) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        border = BorderStroke(1.dp, SelectedLine),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = Muted)
    ) { Text("동승자 정보 등록", fontSize = 16.sp) }
}

@Composable
private fun MemberCard(
    name: String,
    subtitle: String,
    avatarColor: Color,
    driver: Boolean = false,
    enabled: Boolean = false,
    connectionId: String? = null,
    onRemove: () -> Unit = {}
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = Color.White,
        border = BorderStroke(1.dp, Line),
        shadowElevation = 2.dp
    ) {
        Row(
            Modifier.padding(horizontal = 22.dp, vertical = 18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            PersonAvatar(avatarColor)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(name, color = Ink, fontSize = 21.sp, fontWeight = FontWeight.Medium)
                Text(subtitle, color = Muted, fontSize = 15.sp)
                if (connectionId != null) {
                    SelectionContainer {
                        Text(connectionId, color = Muted, fontSize = 12.sp)
                    }
                }
            }
            if (driver) {
                Surface(
                    shape = CircleShape,
                    color = Color(0xFFEDF5F0),
                    border = BorderStroke(1.dp, DriverGreen)
                ) {
                    Text(
                        "운전자",
                        color = DriverGreen,
                        fontSize = 15.sp,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }
            } else {
                Button(
                    onClick = onRemove,
                    enabled = enabled,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF696968)),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(
                        horizontal = 18.dp,
                        vertical = 8.dp
                    )
                ) { Text("삭제", fontSize = 15.sp) }
            }
        }
    }
}

@Composable
private fun PersonAvatar(color: Color) {
    Canvas(Modifier.size(60.dp)) {
        drawCircle(color)
        drawCircle(
            Color.White,
            radius = size.width * 0.15f,
            center = Offset(
                size.width * 0.5f,
                size.height * 0.38f
            )
        )
        drawOval(
            Color.White,
            topLeft = Offset(size.width * 0.25f, size.height * 0.61f),
            size = Size(
                size.width * 0.5f,
                size.height * 0.23f
            )
        )
    }
}

@Composable
private fun BluetoothAvatar() {
    Canvas(Modifier.size(60.dp)) {
        drawCircle(Color(0xFFCDCDCD))
        val path = Path().apply {
            moveTo(size.width * 0.47f, size.height * 0.30f)
            lineTo(size.width * 0.64f, size.height * 0.43f)
            lineTo(size.width * 0.37f, size.height * 0.65f)
            moveTo(size.width * 0.47f, size.height * 0.30f)
            lineTo(size.width * 0.47f, size.height * 0.70f)
            lineTo(size.width * 0.64f, size.height * 0.57f)
            lineTo(size.width * 0.37f, size.height * 0.35f)
        }
        drawPath(
            path,
            Color(0xFF707070),
            style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round)
        )
    }
}

@Composable
private fun RuleCard(rule: SplitRule, selected: Boolean, enabled: Boolean, onSelect: () -> Unit) {
    Surface(
        onClick = onSelect,
        enabled = enabled,
        shape = RoundedCornerShape(12.dp),
        color = if (selected) Color.White else Soft,
        border = BorderStroke(if (selected) 2.dp else 1.dp, if (selected) Ink else Line),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(22.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                (if (selected) "●  " else "○  ") + rule.title,
                color = Ink,
                fontSize = 20.sp,
                fontWeight = FontWeight.Medium
            )
            Text(rule.description, color = Muted, fontSize = 15.sp)
        }
    }
}

@Composable
private fun RoutePreview() {
    Column(Modifier.fillMaxWidth().background(Soft, RoundedCornerShape(12.dp)).padding(22.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("출발", color = Ink)
            Text("중간 합류", color = Muted)
            Text("도착", color = Ink)
        }
        Canvas(Modifier.fillMaxWidth().height(60.dp)) {
            val y = size.height / 2
            drawLine(
                SelectedLine,
                Offset(12.dp.toPx(), y),
                Offset(size.width - 12.dp.toPx(), y),
                strokeWidth = 3.dp.toPx()
            )
            listOf(12.dp.toPx(), size.width * 0.4f, size.width - 12.dp.toPx()).forEach { x ->
                drawCircle(Color.White, 8.dp.toPx(), Offset(x, y))
                drawCircle(Ink, 8.dp.toPx(), Offset(x, y), style = Stroke(2.dp.toPx()))
            }
        }
    }
}

@Composable
private fun Amounts(amounts: Map<String, Long>, members: List<RideMember>) {
    SectionTitle("1인당 정산 금액", "예시 비용 3,300원 · 2개 구간")
    amounts.forEach { (id, amount) ->
        val driver = id == SettlementCalculator.DRIVER
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            PersonAvatar(
                if (driver) {
                    DriverGreen
                } else {
                    AvatarColors[
                        members.indexOfFirst {
                            it.id ==
                                id
                        }.coerceAtLeast(0) %
                            AvatarColors.size
                    ]
                }
            )
            Text(
                if (driver) "나 (운전자)" else members.firstOrNull { it.id == id }?.name.orEmpty(),
                color = Ink,
                modifier = Modifier.weight(1f),
                fontSize = 20.sp
            )
            Text(won(amount), color = Ink, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        }
        LinearProgressIndicator(
            progress = {
                amount / 3300f
            },
            modifier = Modifier.fillMaxWidth().height(
                8.dp
            ),
            color = if (driver) SelectedLine else Ink,
            trackColor = Soft
        )
    }
    HorizontalDivider(color = Line)
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text("합계", color = Muted, fontSize = 20.sp)
        Text(won(amounts.values.sum()), color = Ink, fontSize = 28.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun SessionFooter(
    status: SessionStatus,
    riderCount: Int,
    parked: Boolean,
    onParkedChange: (Boolean) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        HorizontalDivider(color = Line)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                Modifier.size(8.dp).background(
                    if (status ==
                        SessionStatus.Open
                    ) {
                        DriverGreen
                    } else {
                        Muted
                    },
                    CircleShape
                )
            )
            Text(
                when (status) {
                    SessionStatus.Open -> "탑승 연결 대기 · 동승 ${riderCount}명"
                    SessionStatus.Closed -> "탑승 연결 대기 중"
                    SessionStatus.Unavailable -> "블루투스 권한과 연결 상태를 확인해 주세요"
                },
                color = Muted,
                modifier = Modifier.weight(1f),
                fontSize = 14.sp
            )
            Checkbox(checked = parked, onCheckedChange = onParkedChange)
            Text("정차 확인", color = Ink, fontSize = 15.sp)
        }
        Text(
            "차량 센서 미연동 · 실제 정차 중에만 편집하세요. 자동 주행 잠금은 아직 지원하지 않습니다.",
            color = Muted,
            fontSize = 13.sp
        )
        if (riderCount > 0) Text("탑승 중에는 그룹과 규칙 편집이 잠깁니다.", color = Muted, fontSize = 13.sp)
    }
}

@Composable
private fun RegistrationDialog(
    member: Boolean,
    editable: Boolean,
    onDismiss: () -> Unit,
    onSave: (String, String) -> Unit
) {
    var name by rememberSaveable { mutableStateOf("") }
    var phone by rememberSaveable { mutableStateOf("") }
    var consent by rememberSaveable { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color.White,
        shape = RoundedCornerShape(24.dp),
        title = {
            SectionTitle(
                if (member) "동승자 등록" else "새 그룹 만들기",
                if (member) "함께 탈 동승자의 정보를 입력해 주세요." else "자주 함께하는 사람들을 한곳에 모아 보세요."
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                OutlinedTextField(name, {
                    name = it
                }, label = { Text(if (member) "이름" else "그룹 이름") }, singleLine = true)
                if (member) {
                    OutlinedTextField(
                        phone,
                        {
                            phone = it
                        },
                        label = {
                            Text("전화번호")
                        },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Phone
                        ),
                        singleLine = true
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(consent, { consent = it })
                        Text("동승자가 이름·전화번호의 차량 내 저장에 동의했습니다.", color = Ink)
                    }
                    Text(
                        "저장 후 연결 ID를 동승자 앱에서 사용해 주세요. 자동 기기 검색과 문자 발송은 아직 지원하지 않습니다.",
                        color = Muted,
                        fontSize = 13.sp
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(name, phone) },
                enabled =
                editable &&
                    name.isNotBlank() &&
                    (!member || phone.filter(Char::isDigit).length in 10..11 && consent),
                colors = ButtonDefaults.buttonColors(containerColor = Ink)
            ) { Text(if (member) "등록하기" else "만들기") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("취소", color = Muted) } }
    )
}

@Composable
private fun SectionTitle(title: String, subtitle: String) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(title, color = Ink, fontSize = 28.sp, fontWeight = FontWeight.Bold)
        Text(subtitle, color = Muted, fontSize = 15.sp)
    }
}

@Composable
private fun PrimaryButton(text: String, enabled: Boolean, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(containerColor = Ink)
    ) {
        Text(text, fontSize = 18.sp)
    }
}

@Composable
private fun Panel(content: @Composable ColumnScope.() -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = Color.White,
        contentColor = Ink,
        border = BorderStroke(1.5.dp, SelectedLine),
        shadowElevation = 4.dp
    ) {
        Column(
            Modifier.padding(30.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
            content = content
        )
    }
}

@Composable
private fun TwoColumns(
    wide: Boolean,
    firstWeight: Float = 0.5f,
    first: @Composable () -> Unit,
    second: @Composable () -> Unit
) {
    if (wide) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(36.dp)) {
            Box(Modifier.weight(firstWeight)) { first() }
            Box(Modifier.weight(1f - firstWeight)) { second() }
        }
    } else {
        Column(verticalArrangement = Arrangement.spacedBy(24.dp)) {
            first()
            second()
        }
    }
}
