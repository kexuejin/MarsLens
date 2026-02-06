package com.kapp.xloggui.di

import com.kapp.xloggui.data.parser.XlogParser
import com.kapp.xloggui.domain.DesktopFilePicker
import com.kapp.xloggui.domain.FilePicker
import com.kapp.xloggui.domain.XlogDecoder
import org.koin.core.module.Module
import org.koin.dsl.module

actual fun platformModule(): Module = module {
    single<XlogDecoder> { XlogParser() }
    single<FilePicker> { DesktopFilePicker() }
}
