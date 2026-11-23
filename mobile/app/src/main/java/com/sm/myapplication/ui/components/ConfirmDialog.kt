package com.sm.myapplication.ui.components

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import com.sm.myapplication.ui.theme.AccentInk
import com.sm.myapplication.ui.theme.CardWhite
import com.sm.myapplication.ui.theme.Destructive
import com.sm.myapplication.ui.theme.LabelSecondary

/** 삭제처럼 되돌릴 수 없는 동작 전에 한 번 묻는 창. 설정 화면의 로그아웃 확인과 같은 모양이다. */
@Composable
fun DestructiveConfirmDialog(
    title: String,
    message: String,
    confirmText: String = "삭제",
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(20.dp),
        containerColor = CardWhite,
        title = { Text(title, style = MaterialTheme.typography.titleMedium) },
        text = { Text(message, style = MaterialTheme.typography.bodyMedium, color = LabelSecondary) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(confirmText, color = Destructive, style = MaterialTheme.typography.titleSmall)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("취소", color = AccentInk, style = MaterialTheme.typography.titleSmall)
            }
        },
    )
}
