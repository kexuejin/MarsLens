package com.kapp.marslens.di

import com.kapp.marslens.data.parser.XlogParser
import com.kapp.marslens.domain.DesktopFilePicker
import com.kapp.marslens.domain.FilePicker
import com.kapp.marslens.domain.XlogDecoder
import org.koin.core.module.Module
import org.koin.dsl.module

actual fun platformModule(): Module = module {
    single<XlogDecoder> { XlogParser() }
    single<FilePicker> { DesktopFilePicker() }
}
