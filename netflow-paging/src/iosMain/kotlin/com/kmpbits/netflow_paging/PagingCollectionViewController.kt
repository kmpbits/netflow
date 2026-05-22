package com.kmpbits.netflow_paging

import androidx.paging.CombinedLoadStates
import androidx.paging.PagingData
import androidx.paging.PagingDataEvent
import androidx.paging.PagingDataPresenter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch

// Making abstract causes the compilation error "Non-final Kotlin subclasses of Objective-C classes are not yet supported".
class PagingCollectionViewController<T : Any> {

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var submitJob: Job? = null

    // Snapshot is captured inside presentPagingDataEvent right after pageStore is written,
    // guaranteeing it reflects the latest data. Calling snapshot() from outside (getItems)
    // can race with background updates on iOS, producing a stale or empty list.
    private var cachedItems: List<T> = emptyList()

    private val _onPagesUpdatedFlow = MutableSharedFlow<Unit>(
        replay = 1,
        extraBufferCapacity = 1,
    )

    private val presenter = object : PagingDataPresenter<T>(
        mainContext = Dispatchers.Main
    ) {
        override suspend fun presentPagingDataEvent(event: PagingDataEvent<T>) {
            cachedItems = snapshot().items
            _onPagesUpdatedFlow.emit(Unit)
        }
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
        val index = cachedItems.size - 1
        if (index < 0) return
        presenter[index]
    }

    fun getItem(index: Int): T? = cachedItems.getOrNull(index)

    fun getItems(): List<T> = cachedItems

    val loadStateFlow: StateFlow<CombinedLoadStates?> = presenter.loadStateFlow

    val onPagesUpdatedFlow: Flow<Unit> = _onPagesUpdatedFlow.asSharedFlow()

    fun clearScope() = scope.cancel()
}