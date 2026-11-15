package com.example.hamkketa.feature.home.presentation

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Checkbox
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.hamkketa.domain.ride.RideMember
import java.util.UUID

@Composable
internal fun PassengerRegistrationScreen(
    wide: Boolean,
    enabled: Boolean,
    onCancel: () -> Unit,
    onRegister: (RideMember) -> Unit
) {
    var device by rememberSaveable { mutableStateOf<String?>(null) }
    var name by rememberSaveable { mutableStateOf("") }
    var phone by rememberSaveable { mutableStateOf("") }
    var recordingConsent by rememberSaveable { mutableStateOf(false) }
    var settlementConsent by rememberSaveable { mutableStateOf(false) }
    val valid =
        enabled &&
            device != null &&
            name.isNotBlank() &&
            phone.filter(Char::isDigit).length in 10..11 &&
            recordingConsent &&
            settlementConsent
    TwoColumns(wide, first = {
        Column(Modifier.padding(top = 24.dp), verticalArrangement = Arrangement.spacedBy(24.dp)) {
            Heading("새로운 동승자의 폰을 연결하세요.")
            Surface(
                color = Soft,
                border = BorderStroke(1.dp, Line),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(
                    Modifier.fillMaxWidth().padding(26.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        "함께타 앱 → 블루투스 연결",
                        color = Ink,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text("동승자의 함께타 앱에서 차량을 선택해 주세요.", color = Muted)
                }
            }
            Heading("찾은 기기", "UI 확인용 예시 기기 · 실제 검색 결과가 아닙니다")
            listOf("iPhone 16", "iPhone 14 Pro").forEachIndexed { index, item ->
                Surface(
                    onClick = { device = item },
                    enabled = enabled,
                    color = Color.White,
                    shape = RoundedCornerShape(12.dp),
                    shadowElevation = 2.dp,
                    border = BorderStroke(
                        if (device ==
                            item
                        ) {
                            2.dp
                        } else {
                            1.dp
                        },
                        if (device == item) Ink else Line
                    )
                ) {
                    Row(
                        Modifier.fillMaxWidth().padding(22.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        BluetoothAvatar(if (index == 0) Color(0xFF9D4147) else Color(0xFF409F93))
                        Text(item, color = Ink, fontSize = 22.sp, modifier = Modifier.weight(1f))
                        if (device == item) Text("선택됨", color = Ink)
                    }
                }
            }
        }
    }, second = {
        Panel {
            Heading("누구의 휴대폰인가요?", device ?: "왼쪽에서 연결할 기기를 선택해 주세요.")
            Text("이름", color = Ink, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            OutlinedTextField(
                name,
                {
                    name = it
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = enabled,
                placeholder = { Text("예: 반지민") },
                singleLine = true
            )
            Spacer(Modifier.height(8.dp))
            Text("정산 내역을 받을 전화번호", color = Ink, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            OutlinedTextField(
                phone,
                {
                    phone = it
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = enabled,
                placeholder = { Text("010 - 2384 - 0792") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone)
            )
            Spacer(Modifier.height(8.dp))
            Heading("동승자 동의")
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(recordingConsent, { recordingConsent = it }, enabled = enabled)
                Text("(필수) 탑승·하차 위치와 거리 기록 및 이름·전화번호 저장에 동의", color = Ink)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(settlementConsent, { settlementConsent = it }, enabled = enabled)
                Text("(필수) 카카오톡으로 정산 내역 받기에 동의", color = Ink)
            }
            Text(
                "현재는 UI 미리보기로, 실제 기기 연결·위치 기록·카카오톡 발송은 수행하지 않습니다.",
                color = Muted,
                fontSize = 13.sp
            )
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                ActionButton("취소하기", Modifier.weight(1f), outlined = true, onClick = onCancel)
                ActionButton("등록하기", Modifier.weight(1.7f), enabled = valid) {
                    onRegister(
                        RideMember(
                            UUID.randomUUID().toString(),
                            name.trim(),
                            phone.filter(Char::isDigit),
                            device.orEmpty(),
                            recordingConsent,
                            settlementConsent
                        )
                    )
                }
            }
        }
    })
}
