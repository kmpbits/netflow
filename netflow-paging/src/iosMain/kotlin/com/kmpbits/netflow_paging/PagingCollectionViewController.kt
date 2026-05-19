package com.kmpbits.netflow_paging

import androidx.paging.CombinedLoadStates
import androidx.paging.PagingData
import androidx.paging.PagingDataEvent
import androidx.paging.PagingDataPresenter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

// Making abstract causes the compilation error "Non-final Kotlin subclasses of Objective-C classes are not yet supported".
class PagingCollectionViewController<T : Any> {

    private val scope = CoroutineScope(Dispatchers.Main)
    private var submitJob: Job? = null

    private val presenter = object : PagingDataPresenter<T>(
        mainContext = Dispatchers.Main
    ) {
        override suspend fun presentPagingDataEvent(event: PagingDataEvent<T>) = Unit
    }

    fun submitData(pagingData: PagingData<T>) {
        submitJob?.cancel()
        submitJob = scope.launch {
            presenter.collectFrom(pagingData)
        }
    }

    fun retry() = presenter.retry()

    fun refresh() = presenter.refresh()

    fun loadNextPage() {
        val index = presenter.size - 1
        // ignore the first time load with -1
        if (index < 0) return
        presenter[index]
    }

    fun getItem(index: Int): T? = presenter[index]

    fun getItems(): List<T> = presenter.snapshot().items

    val loadStateFlow: StateFlow<CombinedLoadStates?> = presenter.loadStateFlow

    val onPagesUpdatedFlow: Flow<Unit> = presenter.onPagesUpdatedFlow

    fun clearScope() = scope.cancel()
}