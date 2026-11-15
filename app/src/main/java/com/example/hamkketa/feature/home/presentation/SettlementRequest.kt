package com.example.hamkketa.feature.home.presentation

import android.content.ClipData
import android.content.ClipboardManager
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.example.hamkketa.domain.ride.TripCalculator
import com.example.hamkketa.domain.ride.TripRecord

@Composable
internal fun SettlementRequest(records: List<TripRecord>, period: String) {
    val context = LocalContext.current
    var copied by remember(records, period) { mutableStateOf(false) }
    val totals = linkedMapOf<String, Long>()
    val names = mutableMapOf<String, String>()
    val consent = mutableMapOf<String, Boolean>()
    val valid = records.filter { !it.interrupted && it.endedAt != null }
    valid.forEach { record ->
        val result = TripCalculator.calculate(record)
        record.group.members.forEach { member ->
            names[member.id] = member.name
            totals[member.id] = (totals[member.id] ?: 0) + (result.amounts[member.id] ?: 0)
            consent[member.id] = (consent[member.id] ?: true) && member.settlementConsent
        }
    }
    Heading("$period 동승자별 누적", "정산 보류 기록은 누적 금액과 요청에서 제외합니다.")
    totals.forEach { (id, total) ->
        Text("${names[id]} · ${won(total)}")
        TextButton(enabled = consent[id] == true && total > 0, onClick = {
            val source = if (valid.any { it.settings.simulation }) " · 테스트 기록 포함" else ""
            val text = "[함께타 $period$source]\n${names[id]}님 부담액: ${won(total)}\n" +
                "실제 운행 비용 분담이며 수수료는 없습니다. 운전자에게 개인적으로 송금해 주세요."
            context.getSystemService(ClipboardManager::class.java)
                .setPrimaryClip(ClipData.newPlainText("함께타 정산 요청", text))
            copied = true
        }) { Text("${names[id]} 정산 요청 복사") }
    }
    if (copied) {
        Text("복사했어요. 카카오톡 등에 직접 전달해 주세요. 자동 발송과 앱 내 송금은 제공하지 않습니다.")
    } else {
        Text("동의한 동승자의 요청 내용을 복사할 수 있어요. 카카오톡 자동 발송은 연동 전입니다.")
    }
}
