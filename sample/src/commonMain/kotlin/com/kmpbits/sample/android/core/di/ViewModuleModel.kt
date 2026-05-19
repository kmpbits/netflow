package com.kmpbits.sample.android.core.di

import com.kmpbits.sample.android.presentation.MainViewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val viewModelModule = module {
    viewModelOf(::MainViewModel)
}