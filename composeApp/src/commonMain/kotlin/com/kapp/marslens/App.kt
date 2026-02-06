package com.kapp.marslens

import androidx.compose.runtime.Composable
import com.kapp.marslens.ui.theme.AppTheme
import com.kapp.marslens.ui.LogViewerScreen
import org.koin.compose.KoinContext

@Composable
fun App() {
    AppTheme {
        KoinContext {
            LogViewerScreen()
        }
    }
}
