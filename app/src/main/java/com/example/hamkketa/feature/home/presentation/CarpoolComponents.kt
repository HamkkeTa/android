package com.example.hamkketa.feature.home.presentation

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.hamkketa.domain.ride.RideMember
import java.text.NumberFormat
import java.util.Locale

internal val Ink = Color(0xFF3C3C3B)
internal val Muted = Color(0xFF808080)
internal val Soft = Color(0xFFF5F5F5)
internal val Line = Color(0xFFDEDEDE)
internal val Outline = Color(0xFFB9B9B9)
internal val Green = Color(0xFF069B77)
internal val Purple = Color(0xFF6050BC)
internal val Orange = Color(0xFFD98200)
internal fun won(value: Long) = NumberFormat.getNumberInstance(Locale.KOREA).format(value) + "원"

@Composable
internal fun Panel(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Surface(
        modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        color = Color.White,
        contentColor = Ink,
        border = BorderStroke(1.5.dp, Outline),
        shadowElevation = 4.dp
    ) {
        Column(
            Modifier.padding(28.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
            content = content
        )
    }
}

@Composable
internal fun TwoColumns(
    wide: Boolean,
    leftWeight: Float = 0.43f,
    first: @Composable () -> Unit,
    second: @Composable () -> Unit
) {
    if (wide) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(34.dp)) {
            Box(Modifier.weight(leftWeight)) { first() }
            Box(Modifier.weight(1f - leftWeight)) { second() }
        }
    } else {
        Column(verticalArrangement = Arrangement.spacedBy(24.dp)) {
            first()
            second()
        }
    }
}

@Composable
internal fun Heading(title: String, subtitle: String? = null) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(title, color = Ink, fontSize = 28.sp, fontWeight = FontWeight.Bold)
        if (subtitle != null) Text(subtitle, color = Muted, fontSize = 15.sp)
    }
}

@Composable
internal fun ActionButton(
    text: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    outlined: Boolean = false,
    onClick: () -> Unit
) {
    if (outlined) {
        OutlinedButton(
            onClick,
            modifier.heightIn(min = 52.dp),
            enabled = enabled,
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, Outline),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = Ink)
        ) {
            Text(text, fontSize = 18.sp)
        }
    } else {
        Button(
            onClick,
            modifier.heightIn(min = 52.dp),
            enabled = enabled,
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Ink),
            contentPadding = PaddingValues(horizontal = 18.dp, vertical = 12.dp)
        ) {
            Text(text, fontSize = 18.sp)
        }
    }
}

@Composable
internal fun ChoiceChip(
    text: String,
    selected: Boolean,
    enabled: Boolean = true,
    color: Color = Ink,
    onClick: () -> Unit
) {
    FilterChip(
        selected,
        onClick,
        enabled = enabled,
        label = { Text(text, fontSize = 17.sp) },
        shape = CircleShape,
        border = BorderStroke(1.dp, if (selected) color else Line),
        colors = FilterChipDefaults.filterChipColors(
            containerColor = Color.White,
            labelColor = Muted,
            selectedContainerColor = color.copy(alpha = 0.09f),
            selectedLabelColor = color
        )
    )
}

@Composable
internal fun PersonAvatar(color: Color) {
    Canvas(Modifier.size(58.dp)) {
        drawCircle(color)
        drawCircle(Color.White, size.width * 0.15f, Offset(size.width * 0.5f, size.height * 0.38f))
        drawOval(
            Color.White,
            Offset(size.width * 0.25f, size.height * 0.61f),
            Size(
                size.width * 0.5f,
                size.height * 0.23f
            )
        )
    }
}

@Composable
internal fun BluetoothAvatar(color: Color = Color(0xFFCCCCCC)) {
    Canvas(Modifier.size(58.dp)) {
        drawCircle(color)
        val path = Path().apply {
            moveTo(size.width * 0.47f, size.height * 0.30f)
            lineTo(size.width * 0.64f, size.height * 0.43f)
            lineTo(size.width * 0.37f, size.height * 0.65f)
            moveTo(size.width * 0.47f, size.height * 0.30f)
            lineTo(size.width * 0.47f, size.height * 0.70f)
            lineTo(size.width * 0.64f, size.height * 0.57f)
            lineTo(size.width * 0.37f, size.height * 0.35f)
        }
        drawPath(path, Color.White, style = Stroke(2.5.dp.toPx(), cap = StrokeCap.Round))
    }
}

@Composable
internal fun MemberCard(
    member: RideMember,
    index: Int = 0,
    driver: Boolean = false,
    enabled: Boolean = true,
    amount: Long? = null,
    onRemove: (() -> Unit)? = null
) {
    Surface(
        Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = Color.White,
        border = BorderStroke(1.dp, Line),
        shadowElevation = 2.dp
    ) {
        Row(
            Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            PersonAvatar(
                if (driver) {
                    Green
                } else if (index % 2 == 0) {
                    Purple
                } else {
                    Orange
                }
            )
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(member.name, color = Ink, fontSize = 22.sp, fontWeight = FontWeight.Medium)
                if (amount == null || member.deviceName.endsWith("회 탑승")) {
                    Text(
                        if (driver) {
                            "운전자"
                        } else {
                            member.deviceName.ifEmpty {
                                "동승자"
                            }
                        },
                        color = Muted,
                        fontSize = 15.sp
                    )
                }
            }
            if (driver) {
                Surface(
                    shape = CircleShape,
                    color = Green.copy(alpha = 0.08f),
                    border = BorderStroke(1.dp, Green)
                ) {
                    Text(
                        "운전자",
                        color = Green,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                    )
                }
            }
            if (amount != null) Text(won(amount), fontSize = 21.sp, color = Ink)
            if (onRemove !=
                null
            ) {
                TextButton(onRemove, enabled = enabled) {
                    Text("삭제", color = if (enabled) Ink else Muted)
                }
            }
        }
    }
}

@Composable
internal fun ScreenTitle(title: String, subtitle: String? = null, onBack: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            TextButton(onBack) { Text("〈", color = Ink, fontSize = 34.sp) }
            Text(title, color = Ink, fontWeight = FontWeight.Bold, fontSize = 42.sp)
        }
        if (subtitle !=
            null
        ) {
            Text(subtitle, color = Muted, modifier = Modifier.padding(start = 58.dp))
        }
    }
}
