package com.sm.myapplication.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sm.myapplication.ui.theme.AccentGreen
import com.sm.myapplication.ui.theme.AccentInk
import com.sm.myapplication.ui.theme.AccentTint
import com.sm.myapplication.ui.theme.CardWhite
import com.sm.myapplication.ui.theme.Destructive
import com.sm.myapplication.ui.theme.LabelPrimary
import com.sm.myapplication.ui.theme.LabelSecondary
import com.sm.myapplication.ui.theme.LabelTertiary
import com.sm.myapplication.ui.theme.Separator

@Composable
fun Modifier.pressable(enabled: Boolean = true, onClick: () -> Unit): Modifier {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed && enabled) 0.97f else 1f, label = "pressScale")
    return this
        .scale(scale)
        .then(
            Modifier.clickable(
                interactionSource = interaction,
                indication = null,
                enabled = enabled,
                onClick = onClick,
            )
        )
}

@Composable
fun NavBar(
    title: String? = null,
    onBack: (() -> Unit)? = null,
    backLabel: String? = null,
    actionLabel: String? = null,
    actionColor: Color = AccentInk,
    actionEnabled: Boolean = true,
    onAction: (() -> Unit)? = null,
    titleColor: Color = LabelPrimary,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(44.dp),
        contentAlignment = Alignment.Center,
    ) {
        if (title != null) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = titleColor,
            )
        }
        if (onBack != null) {
            Row(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .heightIn(min = 44.dp)
                    .pressable(onClick = onBack)
                    .padding(start = 8.dp, end = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowLeft,
                    contentDescription = "뒤로",
                    tint = AccentInk,
                    modifier = Modifier.size(26.dp),
                )
                if (backLabel != null) {
                    Text(backLabel, color = AccentInk, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
        if (actionLabel != null && onAction != null) {
            Text(
                text = actionLabel,
                color = if (actionEnabled) actionColor else LabelTertiary,
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .heightIn(min = 44.dp)
                    .pressable(enabled = actionEnabled, onClick = onAction)
                    .padding(horizontal = 16.dp),
            )
        }
    }
}

@Composable
fun LargeTitle(title: String, caption: String? = null, modifier: Modifier = Modifier) {
    Column(modifier = modifier.padding(start = 20.dp, end = 20.dp, top = 2.dp, bottom = 12.dp)) {
        if (caption != null) {
            Text(caption, style = MaterialTheme.typography.bodySmall, color = LabelSecondary)
            Spacer(Modifier.height(2.dp))
        }
        Text(title, style = MaterialTheme.typography.headlineLarge, color = LabelPrimary)
    }
}

@Composable
fun SummaryHeader(
    summary: String,
    highlight: String? = null,
    sub: String? = null,
    modifier: Modifier = Modifier,
    trailing: @Composable (() -> Unit)? = null,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = summary,
                    style = MaterialTheme.typography.titleLarge,
                    color = LabelPrimary,
                )
                if (highlight != null) {
                    Spacer(Modifier.width(5.dp))
                    Text(
                        text = highlight,
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = AccentInk,
                    )
                }
            }
            if (sub != null) {
                Spacer(Modifier.height(2.dp))
                Text(sub, style = MaterialTheme.typography.bodySmall, color = LabelSecondary)
            }
        }
        if (trailing != null) {
            Spacer(Modifier.width(10.dp))
            trailing()
        }
    }
}

@Composable
fun InitialAvatar(name: String, size: androidx.compose.ui.unit.Dp = 36.dp, modifier: Modifier = Modifier) {
    val initial = name.trim().takeIf { it.isNotEmpty() }?.take(1)
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(Brush.linearGradient(listOf(AccentGreen, AccentInk))),
        contentAlignment = Alignment.Center,
    ) {
        if (initial != null) {
            Text(
                text = initial,
                color = CardWhite,
                fontWeight = FontWeight.Bold,
                fontSize = (size.value * 0.42f).sp,
            )
        } else {
            Icon(
                Icons.Rounded.Person,
                contentDescription = "프로필",
                tint = CardWhite,
                modifier = Modifier.size(size * 0.55f),
            )
        }
    }
}

@Composable
fun SectionHeader(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium,
        color = LabelSecondary,
        modifier = modifier.padding(start = 24.dp, end = 24.dp, top = 16.dp, bottom = 7.dp),
    )
}

@Composable
fun InsetCard(
    modifier: Modifier = Modifier,
    horizontal: androidx.compose.ui.unit.Dp = 16.dp,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = horizontal)
            .clip(RoundedCornerShape(16.dp))
            .background(CardWhite),
        content = content,
    )
}

@Composable
fun RowDivider(inset: androidx.compose.ui.unit.Dp = 14.dp) {
    Box(
        Modifier
            .fillMaxWidth()
            .padding(start = inset)
            .height(0.5.dp)
            .background(Separator)
    )
}

@Composable
fun SettingRow(
    title: String,
    subtitle: String? = null,
    icon: ImageVector? = null,
    iconColors: List<Color> = listOf(AccentGreen, AccentInk),
    value: String? = null,
    showChevron: Boolean = false,
    onClick: (() -> Unit)? = null,
    trailing: @Composable (() -> Unit)? = null,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 50.dp)
            .then(if (onClick != null) Modifier.pressable(onClick = onClick) else Modifier)
            .padding(horizontal = 14.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            Box(
                modifier = Modifier
                    .size(30.dp)
                    .clip(RoundedCornerShape(9.dp))
                    .background(Brush.linearGradient(iconColors)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(icon, contentDescription = null, tint = CardWhite, modifier = Modifier.size(17.dp))
            }
            Spacer(Modifier.width(11.dp))
        }
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleSmall, color = LabelPrimary)
            if (subtitle != null) {
                Spacer(Modifier.height(1.dp))
                Text(subtitle, style = MaterialTheme.typography.labelSmall, color = LabelSecondary)
            }
        }
        if (value != null) {
            Text(value, style = MaterialTheme.typography.bodySmall, color = LabelSecondary)
        }
        if (trailing != null) {
            Spacer(Modifier.width(8.dp))
            trailing()
        }
        if (showChevron) {
            Spacer(Modifier.width(4.dp))
            Icon(
                Icons.Rounded.ChevronRight,
                contentDescription = null,
                tint = LabelTertiary,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

@Composable
fun AppSwitch(checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Switch(
        checked = checked,
        onCheckedChange = onCheckedChange,
        colors = SwitchDefaults.colors(
            checkedThumbColor = CardWhite,
            checkedTrackColor = AccentGreen,
            checkedBorderColor = AccentGreen,
            uncheckedThumbColor = CardWhite,
            uncheckedTrackColor = Color(0xFFE3E3E8),
            uncheckedBorderColor = Color(0xFFE3E3E8),
        ),
    )
}

@Composable
fun RoundCheck(checked: Boolean, modifier: Modifier = Modifier, onClick: (() -> Unit)? = null) {
    Box(
        modifier = modifier
            .size(22.dp)
            .clip(CircleShape)
            .then(if (checked) Modifier.background(AccentGreen) else Modifier.border(1.5.dp, LabelTertiary, CircleShape))
            .then(if (onClick != null) Modifier.pressable(onClick = onClick) else Modifier),
        contentAlignment = Alignment.Center,
    ) {
        if (checked) {
            Icon(Icons.Rounded.Check, contentDescription = null, tint = CardWhite, modifier = Modifier.size(15.dp))
        }
    }
}

@Composable
fun StatusPill(text: String, dark: Boolean = false, active: Boolean = true, modifier: Modifier = Modifier) {
    val dot = if (dark) com.sm.myapplication.ui.theme.FocusGreen else AccentGreen
    val fg = if (dark) com.sm.myapplication.ui.theme.FocusGreen else AccentInk
    val bg = if (dark) com.sm.myapplication.ui.theme.FocusGreen.copy(alpha = 0.18f) else AccentTint
    Row(
        modifier = modifier
            .clip(CircleShape)
            .background(if (active) bg else Color(0x14000000))
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Box(
            Modifier
                .size(7.dp)
                .clip(CircleShape)
                .background(if (active) dot else LabelTertiary)
        )
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
            color = if (active) fg else LabelSecondary,
        )
    }
}

@Composable
fun PrimaryButton(
    text: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    leading: @Composable (() -> Unit)? = null,
    onClick: () -> Unit,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(50.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(if (enabled) AccentGreen else AccentGreen.copy(alpha = 0.35f))
            .pressable(enabled = enabled, onClick = onClick),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (leading != null) {
            leading()
            Spacer(Modifier.width(6.dp))
        }
        Text(text, color = CardWhite, style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
fun GhostButton(text: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(50.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(CardWhite)
            .pressable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, color = AccentInk, style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
fun DestructiveButton(text: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(50.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(CardWhite)
            .pressable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, color = Destructive, style = MaterialTheme.typography.labelLarge)
    }
}

fun formatHms(ms: Long): String {
    val totalSec = (ms / 1000).coerceAtLeast(0)
    return "%02d:%02d:%02d".format(totalSec / 3600, (totalSec / 60) % 60, totalSec % 60)
}

fun formatDuration(ms: Long): String {
    val totalSec = (ms / 1000).coerceAtLeast(0)
    val h = totalSec / 3600
    val m = (totalSec / 60) % 60
    return when {
        h > 0 -> "%dh %02dm".format(h, m)
        m > 0 -> "%dm".format(m)
        else -> "%ds".format(totalSec)
    }
}
