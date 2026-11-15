package com.example.hamkketa.feature.home.presentation

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.hamkketa.domain.ride.CostItem
import com.example.hamkketa.domain.ride.RideGroup
import com.example.hamkketa.domain.ride.SplitRule
import com.example.hamkketa.domain.ride.defaultCostItems
import java.util.UUID

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun RulesScreen(
    group: RideGroup,
    wide: Boolean,
    editable: Boolean,
    onCancel: () -> Unit,
    onSave: (SplitRule, Set<String>, List<CostItem>) -> Unit
) {
    var rule by remember(group.id) {
        mutableStateOf(
            if (group.rule ==
                SplitRule.Exempt
            ) {
                SplitRule.Equal
            } else {
                group.rule
            }
        )
    }
    var exempt by remember(group.id) { mutableStateOf(group.exemptIds) }
    var costs by remember(group.id) { mutableStateOf(group.costs) }
    var addingCost by remember { mutableStateOf(false) }
    var costName by remember { mutableStateOf("") }
    TwoColumns(wide, first = {
        Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
            Heading("누가 비용을 나눌까요?")
            listOf(SplitRule.Equal, SplitRule.Passengers).forEach { option ->
                Surface(
                    onClick = { rule = option },
                    enabled = editable,
                    color = if (rule ==
                        option
                    ) {
                        Soft
                    } else {
                        Color.White
                    },
                    border = BorderStroke(
                        if (rule ==
                            option
                        ) {
                            2.dp
                        } else {
                            1.dp
                        },
                        if (rule == option) Outline else Line
                    ),
                    shape = RoundedCornerShape(12.dp),
                    shadowElevation = 2.dp
                ) {
                    Column(
                        Modifier.fillMaxWidth().padding(24.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            option.title,
                            color = Ink,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(option.description, color = Muted)
                    }
                }
            }
            Panel {
                Heading("특정 동승자 면제", "이름을 선택하면 면제, 다시 누르면 해제됩니다.")
                FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    group.members.forEachIndexed { index, member ->
                        ChoiceChip(
                            member.name + if (member.id in
                                exempt
                            ) {
                                " · 면제"
                            } else {
                                ""
                            },
                            member.id in exempt,
                            editable,
                            if (index % 2 == 0) Purple else Orange
                        ) {
                            exempt =
                                if (member.id in exempt) exempt - member.id else exempt + member.id
                        }
                    }
                }
                if (group.members.isEmpty()) Text("등록된 동승자가 없어요.", color = Muted)
                TextButton(
                    onClick = { exempt = emptySet() },
                    enabled =
                    editable && exempt.isNotEmpty()
                ) { Text("면제 모두 해제") }
            }
            Heading("정산에 넣을 비용", "항목을 눌러 포함·제외하거나, ×로 삭제할 수 있어요.")
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                costs.forEach { item ->
                    Row {
                        ChoiceChip(
                            item.name + if (item.included) " ✓" else " 제외",
                            item.included,
                            editable
                        ) {
                            costs =
                                costs.map {
                                    if (it.id ==
                                        item.id
                                    ) {
                                        it.copy(included = !it.included)
                                    } else {
                                        it
                                    }
                                }
                        }
                        TextButton(onClick = {
                            costs = costs.filterNot { it.id == item.id }
                        }, enabled = editable) { Text("× ${item.name} 삭제", fontSize = 12.sp) }
                    }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ActionButton("＋ 비용 추가", enabled = editable, outlined = true) { addingCost = true }
                TextButton(onClick = {
                    costs =
                        costs +
                        defaultCostItems().filter { default ->
                            costs.none {
                                it.name ==
                                    default.name
                            }
                        }
                }, enabled = editable) { Text("기본 항목 복원") }
            }
            if (costs.none {
                    it.included
                }
            ) {
                Text("포함된 비용이 없어요. 현재 규칙은 비용을 청구하지 않는 설정입니다.", color = Muted)
            }
        }
    }, second = {
        Panel {
            Heading("이 규칙이면 이렇게 나눠요", "금액은 UI 예시이며 설정으로 자동 계산되지 않습니다.")
            val people = listOf(PreviewDriver) + group.members
            people.forEachIndexed { index, member ->
                val waived = if (index == 0) rule == SplitRule.Passengers else member.id in exempt
                val color = if (index == 0) {
                    Green
                } else if (index % 2 == 1) {
                    Purple
                } else {
                    Orange
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(
                        member.name + if (index ==
                            0
                        ) {
                            " (운전자)"
                        } else {
                            ""
                        },
                        color = Ink,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(if (waived) "면제" else "1,100원 (예시)", color = Muted, fontSize = 18.sp)
                }
                LinearProgressIndicator(
                    progress = { if (waived) 0f else 0.33f },
                    color = color,
                    trackColor = color.copy(alpha = 0.1f),
                    modifier = Modifier.fillMaxWidth().height(18.dp)
                )
            }
            HorizontalDivider(color = Line)
            Text(
                "포함 비용: ${costs.filter {
                    it.included
                }.joinToString { it.name }.ifEmpty { "없음" }}",
                color = Muted
            )
            if (!editable) Text("자동 시작 중인 그룹의 규칙은 변경할 수 없어요.", color = Muted)
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                ActionButton("취소하기", Modifier.weight(1f), outlined = true, onClick = onCancel)
                ActionButton("규칙 저장", Modifier.weight(1.7f), enabled = editable) {
                    onSave(rule, exempt, costs)
                }
            }
        }
    })
    if (addingCost) {
        AlertDialog(
            onDismissRequest = { addingCost = false },
            title = { Text("정산 비용 추가") },
            text = {
                OutlinedTextField(costName, {
                    costName = it.take(20)
                }, label = { Text("항목 이름 (예: 충전비)") }, singleLine = true)
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        costs = costs + CostItem(UUID.randomUUID().toString(), costName.trim())
                        costName = ""
                        addingCost = false
                    },
                    enabled = costName.isNotBlank() &&
                        costs.none {
                            it.name == costName.trim()
                        }
                ) { Text("추가") }
            },
            dismissButton = { TextButton(onClick = { addingCost = false }) { Text("취소") } }
        )
    }
}
