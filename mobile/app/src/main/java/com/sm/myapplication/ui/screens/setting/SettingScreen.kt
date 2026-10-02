package com.sm.myapplication.ui.screens.setting

import android.net.Uri
import android.widget.MediaController
import android.widget.VideoView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.HelpOutline
import androidx.compose.material.icons.rounded.Flag
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.sm.myapplication.data.datastore.AppPreferences
import com.sm.myapplication.ui.components.AppSwitch
import com.sm.myapplication.ui.components.InsetCard
import com.sm.myapplication.ui.components.NavBar
import com.sm.myapplication.ui.components.PrimaryButton
import com.sm.myapplication.ui.components.RowDivider
import com.sm.myapplication.ui.components.SectionHeader
import com.sm.myapplication.ui.components.SettingRow
import com.sm.myapplication.ui.components.DestructiveButton
import com.sm.myapplication.ui.components.pressable
import com.sm.myapplication.ui.theme.AccentGreen
import com.sm.myapplication.ui.theme.AccentInk
import com.sm.myapplication.ui.theme.AccentTint
import com.sm.myapplication.ui.theme.CardWhite
import com.sm.myapplication.ui.theme.Destructive
import com.sm.myapplication.ui.theme.FillGray
import com.sm.myapplication.ui.theme.Green300
import com.sm.myapplication.ui.theme.Green500
import com.sm.myapplication.ui.theme.Green700
import com.sm.myapplication.ui.theme.Green900
import com.sm.myapplication.ui.theme.GroupedBg
import com.sm.myapplication.ui.theme.LabelPrimary
import com.sm.myapplication.ui.theme.LabelSecondary
import com.sm.myapplication.ui.theme.LabelTertiary
import com.sm.myapplication.ui.theme.SystemBlue
import com.sm.myapplication.ui.theme.SystemBlueLight
import com.sm.myapplication.ui.theme.SystemGrayIcon
import com.sm.myapplication.ui.theme.SystemGrayLight
import com.sm.myapplication.ui.theme.SystemOrange
import com.sm.myapplication.ui.theme.SystemOrangeLight
import com.sm.myapplication.ui.theme.SystemPurple
import com.sm.myapplication.ui.theme.SystemPurpleLight
import com.sm.myapplication.ui.theme.SystemRedLight
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.LocalDate

private val educationOptions = listOf("중학생", "고1", "고2", "고3", "N수생", "대학생")

@Composable
fun SettingScreen(
    onOpenMyInfo: () -> Unit,
    onOpenPrivacy: () -> Unit,
    onOpenHelp: () -> Unit,
    onOpenDDay: () -> Unit,
    onLogout: () -> Unit,
) {
    val context = LocalContext.current
    val prefs = remember { AppPreferences(context.applicationContext) }
    val scope = rememberCoroutineScope()

    var notificationsEnabled by remember { mutableStateOf(false) }
    var showLogoutDialog by remember { mutableStateOf(false) }
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var education by remember { mutableStateOf("") }
    var ddayLabel by remember { mutableStateOf("") }
    var ddayEpochDay by remember { mutableStateOf<Long?>(null) }

    LaunchedEffect(Unit) {
        notificationsEnabled = prefs.notificationsEnabled.first()
        name = prefs.memberName.first()
        email = prefs.email.first()
        education = prefs.education.first()
        ddayLabel = prefs.ddayLabel.first()
        ddayEpochDay = prefs.ddayEpochDay.first()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(GroupedBg),
    ) {
        ProfileHeader(name = name, email = email)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .offset(y = (-26).dp)
                .clip(RoundedCornerShape(topStart = 26.dp, topEnd = 26.dp))
                .background(GroupedBg)
                .verticalScroll(rememberScrollState())
                .padding(top = 16.dp),
        ) {
            InsetCard {
                SettingRow(
                    title = "알림",
                    subtitle = "공부 종료·목표 달성 알림 받기",
                    icon = Icons.Rounded.Notifications,
                    iconColors = listOf(SystemRedLight, Destructive),
                    trailing = {
                        AppSwitch(
                            checked = notificationsEnabled,
                            onCheckedChange = { enabled ->
                                notificationsEnabled = enabled
                                scope.launch { prefs.setNotificationsEnabled(enabled) }
                                if (enabled) NotificationHelper.requestNotificationPermission(context)
                            },
                        )
                    },
                )
            }

            SectionHeader("나의 정보")
            InsetCard {
                SettingRow(
                    title = "프로필",
                    subtitle = "이름, 생년월일, 학력·학년",
                    icon = Icons.Rounded.Person,
                    iconColors = listOf(SystemBlueLight, SystemBlue),
                    value = education.takeIf { it.isNotBlank() },
                    showChevron = true,
                    onClick = onOpenMyInfo,
                )
                RowDivider(inset = 55.dp)
                SettingRow(
                    title = "디데이",
                    subtitle = "목표 날짜까지 남은 일수 표시",
                    icon = Icons.Rounded.Flag,
                    iconColors = listOf(SystemOrangeLight, SystemOrange),
                    value = ddayEpochDay?.let { epoch ->
                        val left = epoch - LocalDate.now().toEpochDay()
                        "$ddayLabel · " + if (left >= 0) "D-$left" else "D+${-left}"
                    } ?: "설정 안 함",
                    showChevron = true,
                    onClick = onOpenDDay,
                )
            }

            SectionHeader("지원")
            InsetCard {
                SettingRow(
                    title = "개인정보 처리방침",
                    subtitle = "수집 항목과 보유 기간",
                    icon = Icons.Rounded.Shield,
                    iconColors = listOf(SystemGrayLight, SystemGrayIcon),
                    showChevron = true,
                    onClick = onOpenPrivacy,
                )
                RowDivider(inset = 55.dp)
                SettingRow(
                    title = "도움말",
                    subtitle = "영상으로 사용법 보기",
                    icon = Icons.AutoMirrored.Rounded.HelpOutline,
                    iconColors = listOf(SystemPurpleLight, SystemPurple),
                    value = "v1.0",
                    showChevron = true,
                    onClick = onOpenHelp,
                )
            }

            Spacer(Modifier.height(18.dp))
            DestructiveButton(
                text = "로그아웃",
                modifier = Modifier.padding(horizontal = 16.dp),
                onClick = { showLogoutDialog = true },
            )
            Spacer(Modifier.height(28.dp))
        }
    }

    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            shape = RoundedCornerShape(20.dp),
            containerColor = CardWhite,
            title = { Text("로그아웃할까요?", style = MaterialTheme.typography.titleMedium) },
            text = { Text("기록한 공부 시간은 그대로 남아 있어요.", style = MaterialTheme.typography.bodyMedium, color = LabelSecondary) },
            confirmButton = {
                TextButton(onClick = { showLogoutDialog = false; onLogout() }) {
                    Text("로그아웃", color = Destructive, style = MaterialTheme.typography.titleSmall)
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) {
                    Text("취소", color = AccentInk, style = MaterialTheme.typography.titleSmall)
                }
            },
        )
    }
}

/** 초록 그라데이션 헤더 + 원형 프로필 + 이름 + 밑줄 이메일. */
@Composable
private fun ProfileHeader(name: String, email: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Brush.linearGradient(listOf(Green500, Green700, Green900))),
    ) {
        // 은은한 광택
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .offset(x = 40.dp, y = (-50).dp)
                .size(190.dp)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        listOf(CardWhite.copy(alpha = 0.22f), Color.Transparent)
                    )
                )
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(top = 6.dp, bottom = 46.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text("설정", style = MaterialTheme.typography.headlineSmall, color = CardWhite)
            Spacer(Modifier.height(14.dp))
            Box(
                modifier = Modifier
                    .size(78.dp)
                    .clip(CircleShape)
                    .background(CardWhite.copy(alpha = 0.28f)),
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    modifier = Modifier
                        .size(70.dp)
                        .clip(CircleShape)
                        .background(Brush.linearGradient(listOf(Color(0xFFD8EFE0), Green300))),
                    contentAlignment = Alignment.Center,
                ) {
                    val initial = name.trim().takeIf { it.isNotEmpty() }?.take(1)
                    if (initial != null) {
                        Text(
                            text = initial,
                            style = MaterialTheme.typography.headlineSmall,
                            color = Green900,
                        )
                    } else {
                        Icon(
                            Icons.Rounded.Person,
                            contentDescription = null,
                            tint = Green900,
                            modifier = Modifier.size(34.dp),
                        )
                    }
                }
            }
            Spacer(Modifier.height(10.dp))
            Text(
                text = name.ifBlank { "이름을 등록해 주세요" },
                style = MaterialTheme.typography.titleMedium,
                color = CardWhite,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = email.ifBlank { "이메일 미등록" },
                style = MaterialTheme.typography.bodySmall,
                color = CardWhite.copy(alpha = 0.9f),
                textDecoration = TextDecoration.Underline,
            )
        }
    }
}

@Composable
fun MyInfoScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val prefs = remember { AppPreferences(context.applicationContext) }
    val scope = rememberCoroutineScope()
    var name by remember { mutableStateOf("") }
    var birth by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var selectedEducation by remember { mutableStateOf(educationOptions.first()) }
    var saved by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        name = prefs.memberName.first()
        birth = prefs.birthDate.first()
        email = prefs.email.first()
        selectedEducation = prefs.education.first()
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(GroupedBg)
            .statusBarsPadding()
            .navigationBarsPadding(),
    ) {
        NavBar(title = "프로필", onBack = onBack, backLabel = "설정")
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
        ) {
            SectionHeader("기본 정보")
            InsetCard {
                FieldRow("이름", name, "홍길동") { name = it; saved = false }
                RowDivider(inset = 16.dp)
                FieldRow("생년월일", birth, "2007.01.01") { birth = it; saved = false }
                RowDivider(inset = 16.dp)
                FieldRow("이메일", email, "example@email.com") { email = it; saved = false }
            }

            SectionHeader("학력 · 학년")
            Column(
                modifier = Modifier.padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                educationOptions.chunked(3).forEach { rowOptions ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        rowOptions.forEach { option ->
                            val selected = option == selectedEducation
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(44.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (selected) AccentGreen else CardWhite)
                                    .pressable { selectedEducation = option; saved = false },
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    option,
                                    style = MaterialTheme.typography.titleSmall,
                                    color = if (selected) CardWhite else LabelPrimary,
                                )
                            }
                        }
                        repeat(3 - rowOptions.size) { Spacer(Modifier.weight(1f)) }
                    }
                }
            }

            Spacer(Modifier.height(24.dp))
            PrimaryButton(
                text = if (saved) "저장했어요" else "저장",
                modifier = Modifier.padding(horizontal = 16.dp),
            ) {
                scope.launch {
                    prefs.saveProfile(name, birth, email, selectedEducation)
                    saved = true
                }
            }
            Spacer(Modifier.height(28.dp))
        }
    }
}

@Composable
private fun FieldRow(label: String, value: String, placeholder: String, onValueChange: (String) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            label,
            style = MaterialTheme.typography.bodyMedium,
            color = LabelPrimary,
            modifier = Modifier.size(width = 84.dp, height = 22.dp),
        )
        Box(Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
            if (value.isEmpty()) {
                Text(placeholder, style = MaterialTheme.typography.bodyMedium, color = LabelTertiary)
            }
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyMedium.copy(color = LabelPrimary),
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
fun PrivacyScreen(onBack: () -> Unit) {
    Column(
        Modifier
            .fillMaxSize()
            .background(GroupedBg)
            .statusBarsPadding()
            .navigationBarsPadding(),
    ) {
        NavBar(title = "개인정보 처리방침", onBack = onBack, backLabel = "설정")
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
        ) {
            SectionHeader("제1조 개인정보의 수집·이용")
            InsetCard {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        "회사는 다음의 목적을 위하여 개인정보를 처리하고 있으며, 다음 목적 이외의 용도로는 이용하지 않습니다.",
                        style = MaterialTheme.typography.bodySmall,
                        color = LabelSecondary,
                    )
                    PrivacyTable()
                    Text(
                        "※ 카카오 등 SNS 계정으로 가입하는 경우, 회사는 해당 서비스 제공자가 보내는 비식별 고유 Key 값으로만 계정을 생성하며 개인정보에 해당하는 정보는 수집하지 않습니다.",
                        style = MaterialTheme.typography.labelSmall,
                        color = LabelSecondary,
                    )
                }
            }

            SectionHeader("보유 및 이용기간")
            InsetCard {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        "회원이 탈퇴하거나 보유목적이 달성된 경우 해당 개인정보를 지체 없이 파기합니다. 다만 아래 법령에 따라 일정 기간 보관할 수 있습니다.",
                        style = MaterialTheme.typography.bodySmall,
                        color = LabelSecondary,
                    )
                    Text("전자상거래 등에서의 소비자보호에 관한 법률", style = MaterialTheme.typography.titleSmall, color = LabelPrimary)
                    Text(
                        "• 계약 또는 청약철회 등에 관한 기록: 5년\n• 대금결제 및 재화 등의 공급에 관한 기록: 5년\n• 소비자의 불만 또는 분쟁처리에 관한 기록: 3년\n• 표시·광고에 관한 기록: 6개월",
                        style = MaterialTheme.typography.bodySmall,
                        color = LabelSecondary,
                    )
                    Text("통신비밀보호법", style = MaterialTheme.typography.titleSmall, color = LabelPrimary)
                    Text(
                        "• 서비스 방문기록, 접속로그, 접속 IP: 3개월",
                        style = MaterialTheme.typography.bodySmall,
                        color = LabelSecondary,
                    )
                }
            }
            Spacer(Modifier.height(28.dp))
        }
    }
}

@Composable
private fun PrivacyTable() {
    val rows = listOf(
        listOf("회원 가입 및 관리", "가입 의사 확인, 서비스 제공, 회원자격 유지·관리, 부정이용 방지, 고충처리", "이메일, 비밀번호, 닉네임", "탈퇴 후 지체없이 파기"),
        listOf("부모 인증", "만 14세 미만 가입 시 법정대리인 인증", "법정대리인 이름·휴대전화 번호", "인증 완료 후 즉시 파기"),
        listOf("서비스 이용", "부정이용 방지, 맞춤 서비스 제공, 이용 통계 분석", "서비스 이용 기록, 접속 IP", "탈퇴 후 지체없이 파기"),
        listOf("유료 서비스", "재화의 구매 및 결제, 요금정산", "결제 기록", "서비스 해지 시까지"),
    )
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(FillGray)
    ) {
        TableRow(listOf("구분", "수집·이용 목적", "수집 항목", "보유 기간"), header = true)
        rows.forEach { TableRow(it) }
    }
}

@Composable
private fun TableRow(cells: List<String>, header: Boolean = false) {
    Row(
        Modifier
            .fillMaxWidth()
            .background(if (header) AccentTint else Color.Transparent)
    ) {
        cells.forEachIndexed { index, text ->
            Text(
                text = text,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = if (header) FontWeight.Bold else FontWeight.Normal,
                ),
                color = if (header) AccentInk else LabelSecondary,
                modifier = Modifier
                    .weight(if (index == 0) 0.9f else 1.7f)
                    .padding(7.dp),
            )
        }
    }
}

@Composable
fun HelpScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val videoResId = remember { context.resources.getIdentifier("help_video", "raw", context.packageName) }

    Column(
        Modifier
            .fillMaxSize()
            .background(GroupedBg)
            .statusBarsPadding()
            .navigationBarsPadding(),
    ) {
        NavBar(title = "도움말", onBack = onBack, backLabel = "설정")
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
        ) {
            SectionHeader("영상으로 사용법 알아보기")
            if (videoResId != 0) {
                AndroidView(
                    factory = {
                        VideoView(it).apply {
                            setMediaController(MediaController(it))
                            setVideoURI(Uri.parse("android.resource://${context.packageName}/$videoResId"))
                            setOnPreparedListener { player -> player.isLooping = true; start() }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .height(220.dp)
                        .clip(RoundedCornerShape(18.dp)),
                )
            } else {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .height(200.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(AccentTint),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("사용법 영상", style = MaterialTheme.typography.titleMedium, color = AccentInk)
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "res/raw/help_video.mp4 를 넣으면 여기서 바로 재생됩니다.",
                            style = MaterialTheme.typography.bodySmall,
                            color = LabelSecondary,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 24.dp),
                        )
                    }
                }
            }

            SectionHeader("자주 묻는 질문")
            InsetCard {
                FaqRow("기본 모드와 순공 모드는 뭐가 다른가요?", "기본 모드는 직접 시작·일시정지합니다. 순공 모드는 전면 카메라로 얼굴을 확인해 자리를 비우면 자동으로 멈춥니다.")
                RowDivider(inset = 16.dp)
                FaqRow("순공 모드 제스처는 어떻게 쓰나요?", "손바닥을 펴면 시작·재개, 주먹을 쥐면 일시정지됩니다.")
                RowDivider(inset = 16.dp)
                FaqRow("기록이 저장되지 않아요.", "1초 미만이거나 12시간을 넘는 세션은 오기록으로 보고 저장하지 않습니다.")
            }
            Spacer(Modifier.height(28.dp))
        }
    }
}

@Composable
private fun FaqRow(question: String, answer: String) {
    Column(Modifier.padding(horizontal = 16.dp, vertical = 13.dp)) {
        Text(question, style = MaterialTheme.typography.titleSmall, color = LabelPrimary)
        Spacer(Modifier.height(3.dp))
        Text(answer, style = MaterialTheme.typography.bodySmall, color = LabelSecondary)
    }
}
