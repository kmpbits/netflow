package com.kmpbits.sample.android.core.di

import com.kmpbits.sample.android.data.repository.TodoRepositoryImpl
import com.kmpbits.sample.android.domain.repository.TodoRepository
import org.koin.core.module.dsl.bind
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

val repositoryModule = module {
    singleOf(::TodoRepositoryImpl) { bind<TodoRepository>() }
}