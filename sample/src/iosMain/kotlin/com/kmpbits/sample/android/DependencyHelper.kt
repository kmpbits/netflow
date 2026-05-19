package com.kmpbits.sample.android

import com.kmpbits.sample.android.presentation.MainViewModel
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

object DependencyHelper : KoinComponent {
    val mainViewModel by inject<MainViewModel>()
}