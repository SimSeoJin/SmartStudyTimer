package com.sm.myapplication

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.sm.myapplication.ui.nav.AppNavHost
import com.sm.myapplication.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        // 앱은 밝은 테마 고정이라, 폰이 다크 모드여도 시스템 바는 밝은 스타일(어두운 아이콘)로 둔다.
        // 기본값(auto)은 다크 모드에서 3버튼 내비게이션 바에 회색 막을 씌워 흰 화면과 어긋났다.
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.light(Color.WHITE, Color.WHITE),
        )
        super.onCreate(savedInstanceState)
        setContent {
            MyApplicationTheme {
                AppNavHost()
            }
        }
    }
}
