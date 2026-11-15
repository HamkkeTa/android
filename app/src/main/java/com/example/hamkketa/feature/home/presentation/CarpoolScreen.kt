package com.example.hamkketa.feature.home.presentation

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import com.example.hamkketa.domain.ride.RideGroup
import com.example.hamkketa.domain.ride.SplitRule
import kotlinx.coroutines.launch

@Composable
fun HomeRoute(modifier: Modifier = Modifier, viewModel: CarpoolViewModel = hiltViewModel()) {
    val data by viewModel.data.collectAsStateWithLifecycle()
    val runningId by viewModel.runningGroupId.collectAsStateWithLifecycle()
    val draft by viewModel.draft.collectAsStateWithLifecycle()
    var page by rememberSaveable { mutableStateOf("그룹") }
    var selectedId by rememberSaveable { mutableStateOf<String?>(null) }
    var ledgerId by rememberSaveable { mutableStateOf<String?>(null) }
    var creating by rememberSaveable { mutableStateOf(false) }
    var summaryId by rememberSaveable { mutableStateOf<String?>(null) }
    var savedTripIds by rememberSaveable { mutableStateOf(setOf<String>()) }
    val group = data.groups.firstOrNull { it.id == selectedId } ?: data.groups.firstOrNull()
    val ledgerGroup = data.groups.firstOrNull { it.id == ledgerId } ?: data.groups.firstOrNull()
    val trips = ledgerGroup?.let { previewTrips(it.id, it.name) }.orEmpty()
    val trip = trips.firstOrNull { it.id == summaryId } ?: trips.firstOrNull()
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val notice: (String) -> Unit = { message -> scope.launch { snackbar.showSnackbar(message) } }
    val back: () -> Unit = { page = if (page == "운행 요약") "장부" else "그룹" }
    BackHandler(page != "그룹", onBack = back)

    BoxWithConstraints(modifier.fillMaxSize().background(Color.White)) {
        val wide = maxWidth >= 840.dp
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState())
                .padding(horizontal = if (wide) 44.dp else 20.dp, vertical = 28.dp),
            verticalArrangement = Arrangement.spacedBy(28.dp)
        ) {
            when (page) {
                "그룹", "장부" -> AppHeader(page, wide) { next -> page = next }
                "동승자 등록" -> ScreenTitle("동승자 등록", onBack = back)
                "분담 규칙" -> ScreenTitle("분담 규칙 설정", group?.name, back)
                "운행 요약" -> ScreenTitle(
                    "운행 요약",
                    trip?.let {
                        "${it.groupName} (${it.date} 07:03 ~ 07:54)"
                    },
                    back
                )
            }
            when (page) {
                "그룹" -> TwoColumns(wide, first = {
                    Column(
                        Modifier.padding(top = if (wide) 65.dp else 0.dp),
                        verticalArrangement = Arrangement.spacedBy(18.dp)
                    ) {
                        data.groups.forEach { item ->
                            GroupCard(
                                item,
                                item.id == selectedId && !creating,
                                runningId,
                                onSelect = {
                                    selectedId = item.id
                                    creating = false
                                },
                                onStart = {
                                    selectedId = item.id
                                    viewModel.start(item.id)
                                },
                                onStop = viewModel::stop,
                                onRules = {
                                    selectedId = item.id
                                    creating = false
                                    page = "분담 규칙"
                                }
                            )
                        }
                        ActionButton(
                            "＋  새 그룹 만들기",
                            Modifier.fillMaxWidth(),
                            enabled =
                            runningId == null,
                            outlined = true,
                            onClick = {
                                viewModel.newDraft()
                                creating = true
                            }
                        )
                    }
                }, second = {
                    Panel {
                        if (creating) {
                            Heading("새 그룹 만들기", "이름을 짓고 함께 탈 동승자를 추가해 주세요.")
                            OutlinedTextField(
                                draft.name,
                                viewModel::draftName,
                                label = {
                                    Text("그룹 이름")
                                },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                        } else {
                            Heading(group?.name ?: "그룹이 없어요", "멤버 구성")
                        }
                        MemberCard(PreviewDriver, driver = true)
                        val members = if (creating) draft.members else group?.members.orEmpty()
                        val locked = !creating && group?.id == runningId
                        members.forEachIndexed { index, member ->
                            MemberCard(member, index, enabled = !locked, onRemove = {
                                if (creating) {
                                    viewModel.removeDraftMember(member.id)
                                } else {
                                    group?.let { viewModel.removeMember(it.id, member.id) }
                                }
                            })
                        }
                        Spacer(Modifier.height(10.dp))
                        if (locked) {
                            Surface(color = Soft, shape = RoundedCornerShape(12.dp)) {
                                Text(
                                    "자동 시작 중에는 동승자를 추가하거나 삭제할 수 없어요. ‘취소하기’로 종료해 주세요.",
                                    color = Muted,
                                    modifier = Modifier.padding(20.dp)
                                )
                            }
                        } else {
                            RegistrationEntry(enabled = creating || group != null) {
                                page = "동승자 등록"
                            }
                        }
                        if (creating) {
                            Text(
                                "기본 규칙: 운전자 포함 · 면제 없음 · 모든 비용 포함",
                                color = Muted,
                                fontSize = 14.sp
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                ActionButton("취소하기", Modifier.weight(1f), outlined = true) {
                                    creating =
                                        false
                                }
                                ActionButton(
                                    "그룹 만들기",
                                    Modifier.weight(1.5f),
                                    enabled = draft.name.isNotBlank()
                                ) {
                                    viewModel.createGroup(draft.name, draft.members)?.let {
                                        selectedId = it
                                        creating = false
                                        viewModel.newDraft()
                                    }
                                }
                            }
                        }
                    }
                })
                "동승자 등록" -> PassengerRegistrationScreen(
                    wide,
                    enabled =
                    creating || group?.id != runningId,
                    onCancel = back,
                    onRegister = { member ->
                        if (creating) {
                            viewModel.addDraftMember(member)
                            page = "그룹"
                        } else {
                            group?.let {
                                if (viewModel.addMember(it.id, member)) page = "그룹"
                            }
                        }
                    }
                )
                "분담 규칙" -> group?.let {
                    RulesScreen(
                        it,
                        wide,
                        editable = runningId != it.id,
                        onCancel = back,
                        onSave = { rule, exempt, costs ->
                            viewModel.saveRule(it.id, rule, exempt, costs)
                            page = "그룹"
                        }
                    )
                }
                "장부" -> LedgerScreen(
                    data.groups,
                    ledgerGroup,
                    trips,
                    savedTripIds,
                    wide,
                    onSelectGroup = { ledgerId = it },
                    onTrip = {
                        summaryId = it
                        page = "운행 요약"
                    }
                )
                "운행 요약" -> trip?.let {
                    SummaryScreen(
                        it,
                        wide,
                        stored = it.id in savedTripIds,
                        onStore = {
                            savedTripIds = savedTripIds + it.id
                            notice("예시 기록을 장부에 모았어요.")
                        },
                        onShare = { notice("UI 미리보기입니다. 실제 카카오톡 발송은 아직 연결되지 않았어요.") }
                    )
                }
            }
            Text(
                if (runningId != null) {
                    "UI 미리보기 · 자동 시작 상태입니다. 실제 주행·비용 계산은 수행하지 않습니다."
                } else {
                    "UI 미리보기 · 기기와 운행 기록·금액은 예시입니다. 실제 주행 중에는 조작하지 마세요."
                },
                color = Muted,
                fontSize = 13.sp
            )
        }
        SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter).padding(24.dp))
    }
}

@Composable
private fun AppHeader(page: String, wide: Boolean, onPage: (String) -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(if (wide) 30.dp else 12.dp)
    ) {
        Text(
            "함께타",
            color = Ink,
            fontSize = if (wide) 76.sp else 34.sp,
            fontWeight = FontWeight.ExtraBold
        )
        listOf("그룹", "장부").forEach { tab ->
            Surface(
                onClick = { onPage(tab) },
                shape = CircleShape,
                color = if (page ==
                    tab
                ) {
                    Ink
                } else {
                    Color.White
                },
                border = BorderStroke(
                    1.dp,
                    if (page ==
                        tab
                    ) {
                        Ink
                    } else {
                        Line
                    }
                )
            ) {
                Text(
                    tab,
                    color = if (page ==
                        tab
                    ) {
                        Color.White
                    } else {
                        Ink
                    },
                    fontSize = if (wide) 24.sp else 18.sp,
                    modifier = Modifier.padding(horizontal = 22.dp, vertical = 10.dp)
                )
            }
        }
    }
}

@Composable
private fun GroupCard(
    group: RideGroup,
    selected: Boolean,
    runningId: String?,
    onSelect: () -> Unit,
    onStart: () -> Unit,
    onStop: () -> Unit,
    onRules: () -> Unit
) {
    val running = runningId == group.id
    Surface(
        onClick = onSelect,
        shape = RoundedCornerShape(12.dp),
        color = if (selected || running) Color.White else Soft,
        border = BorderStroke(
            if (selected ||
                running
            ) {
                2.dp
            } else {
                1.dp
            },
            if (selected || running) Outline else Line
        ),
        shadowElevation = if (selected || running) 4.dp else 0.dp
    ) {
        Column(
            Modifier.fillMaxWidth().padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    group.name,
                    color = Ink,
                    fontSize = 23.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                ActionButton(
                    if (running) "취소하기" else "자동 시작",
                    enabled =
                    runningId == null || running,
                    onClick = if (running) onStop else onStart
                )
            }
            Text(
                "멤버 ${group.members.size + 1}명 · ${if (group.rule == SplitRule.Passengers) "동승자끼리만" else "운전자 포함 균등"}",
                color = Muted,
                fontSize = 15.sp
            )
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    if (running) "●  자동 시작 중" else "시작 전",
                    color = if (running) Green else Muted,
                    fontSize = 13.sp
                )
                TextButton(onClick = onRules) { Text("분담 규칙  〉", color = Ink, fontSize = 15.sp) }
            }
        }
    }
}

@Composable
private fun RegistrationEntry(enabled: Boolean, onClick: () -> Unit) {
    Surface(
        Modifier.fillMaxWidth(),
        color = Soft,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, Line)
    ) {
        BoxWithConstraints {
            val inline = maxWidth >= 500.dp
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    BluetoothAvatar()
                    Text("동승자 등록", color = Muted, fontSize = 21.sp, modifier = Modifier.weight(1f))
                    if (inline) {
                        ActionButton(
                            "블루투스 연결하기",
                            enabled = enabled,
                            outlined = true,
                            onClick = onClick
                        )
                    }
                }
                if (!inline) {
                    ActionButton(
                        "블루투스 연결하기",
                        Modifier.align(Alignment.End),
                        enabled,
                        true,
                        onClick
                    )
                }
            }
        }
    }
}
