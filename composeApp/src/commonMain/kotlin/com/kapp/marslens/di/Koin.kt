package com.kapp.marslens.di

import com.kapp.marslens.ui.MainViewModel
import com.kapp.marslens.ui.FileTreeViewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.core.context.startKoin
import org.koin.core.module.Module
import org.koin.core.module.dsl.bind
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.KoinAppDeclaration
import org.koin.dsl.module
fun initKoin(appDeclaration: KoinAppDeclaration = {}) =
    startKoin {
        appDeclaration()
        modules(commonModule, platformModule())
    }

val commonModule = module {
    viewModelOf(::MainViewModel)
    viewModelOf(::FileTreeViewModel)
}

expect fun platformModule(): Module
