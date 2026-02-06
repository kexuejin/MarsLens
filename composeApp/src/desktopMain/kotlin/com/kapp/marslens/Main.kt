package com.kapp.marslens

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.unit.dp
import com.kapp.marslens.di.initKoin
import com.kapp.marslens.di.platformModule
import com.kapp.marslens.ui.theme.AppTheme

fun main() = application {
    initKoin { 
        // Platform specific config if needed
    }
    
    val windowState = androidx.compose.ui.window.rememberWindowState(
        width = 1200.dp,
        height = 800.dp
    )
    
    Window(
        onCloseRequest = ::exitApplication,
        state = windowState,
        title = "MarsLens - XLog Viewer"
    ) {
        androidx.compose.ui.unit.DpSize(900.dp, 600.dp).let { minSize ->
             // Note: Compose Desktop doesn't have a direct minSize on Window yet without reaching into AWT
             // but we can set the default state.
        }
        AppTheme {
            App()
        }
    }
}
