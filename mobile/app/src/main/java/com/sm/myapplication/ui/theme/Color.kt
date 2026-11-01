package com.sm.myapplication.ui.theme

import androidx.compose.ui.graphics.Color

// ── 순공부 선전 포스터 팔레트 ──────────────────────────────
// 바랜 종이 위에 먹색과 당(黨)의 빨강만 쓰는 2도 인쇄 느낌.

// 종이 (배경·카드). Paper는 어두운 화면 위 글자색으로도 쓴다.
val PaperShade = Color(0xFFEDE3CF)   // 화면 바탕
val Paper = Color(0xFFF8F2E6)        // 카드·버튼 면
val PaperDeep = Color(0xFFE2D5BC)    // 입력칸·표 바탕

// 당의 빨강 (주요 버튼, 선택 상태, 강조)
val PartyRed = Color(0xFFC8372D)
val PartyRedDark = Color(0xFF9E2A22)
val PartyTint = Color(0xFFF1D6CC)    // 빨강 옅은 바탕(칩·하이라이트)

// 먹색 (글자·굵은 선)
val Ink = Color(0xFF1B1B1D)
val AccentInk = Ink

// 순공 인정 (얼굴이 보여 순공으로 집계되는 상태). 포스터 톤에 맞춘 올리브 초록.
val ApprovedGreen = Color(0xFF6E9B4E)

// 구분선
val Separator = Color(0xFFD4C6AB)

// Labels
val LabelPrimary = Ink
val LabelSecondary = Color(0xFF5C5248)
val LabelTertiary = Color(0xFF9A8E80)

// Semantic
val Destructive = PartyRed
val FillGray = PaperDeep

// 설정 아이콘 배경 (포스터 2도 + 보조 톤)
val IconInk = Ink
val IconRed = PartyRed
val IconOlive = ApprovedGreen
val IconUmber = Color(0xFF8A6A4A)
val IconSlate = Color(0xFF5A6470)

// 감시국 (순공모드 어두운 화면)
val FocusBg = Color(0xFF151517)
val FocusSurface = Color(0xFF26262A)
val FocusGreen = ApprovedGreen
val FocusRed = Color(0xFFE0483C)
val FocusCameraTop = Color(0xFF151517)
val FocusCameraBottom = Color(0xFF26262A)

// Surface / Neutral
val White = Color(0xFFFFFFFF)
val Black = Color(0xFF000000)
val Black50 = Color(0x80000000)

// 카카오 로그인
val KakaoYellow = Color(0xFFFEE500)

// 네이버 로그인
val NaverGreen = Color(0xFF03C75A)

// 구글 로그인
val GoogleBlue = Color(0xFF4285F4)
