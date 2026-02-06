package com.kapp.xloggui

import androidx.compose.runtime.Composable
import com.kapp.xloggui.ui.theme.AppTheme
import com.kapp.xloggui.ui.LogViewerScreen
import org.koin.compose.KoinContext

@Composable
fun App() {
    AppTheme {
        KoinContext {
            LogViewerScreen()
        }
    }
}
