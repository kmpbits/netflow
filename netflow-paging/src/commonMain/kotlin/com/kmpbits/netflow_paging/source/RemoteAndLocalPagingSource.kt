package com.kmpbits.netflow_paging.source

import androidx.paging.ExperimentalPagingApi
import androidx.paging.LoadType
import androidx.paging.PagingState
import androidx.paging.RemoteMediator
import com.kmpbits.netflow_core.exceptions.NetFlowException
import com.kmpbits.netflow_core.states.AsyncState
import com.kmpbits.netflow_paging.builder.PagingBuilder
import com.kmpbits.netflow_paging.model.PagingModel
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
@ExperimentalPagingApi
@PublishedApi
internal class RemoteAndLocalPagingSource<ApiType : PagingModel, DisplayType : Any>(
    private val builder: PagingBuilder<ApiType, DisplayType>,
    private val doApiCall: suspend (page: Int) -> AsyncState<List<ApiType>>
) : RemoteMediator<Int, DisplayType>() {

    override suspend fun initialize(): InitializeAction {
        return if (builder.lastUpdatedTimestamp?.invoke() != null &&
            Clock.System.now().toEpochMilliseconds() - builder.lastUpdatedTimestamp?.invoke()!!
            < builder.cacheTimeout.inWholeMilliseconds &&
            builder.refresh.not()
        )
            InitializeAction.SKIP_INITIAL_REFRESH
        else
            InitializeAction.LAUNCH_INITIAL_REFRESH
    }

    override suspend fun load(loadType: LoadType, state: PagingState<Int, DisplayType>): MediatorResult {
        val page = when (loadType) {
            LoadType.REFRESH -> 1
            LoadType.PREPEND -> return MediatorResult.Success(endOfPaginationReached = true)
            LoadType.APPEND -> {
                state.pages.lastOrNull()?.nextKey
                    ?: return MediatorResult.Success(endOfPaginationReached = false)
            }
        }

        val resultState = doApiCall(page)
        val nextPage = page + 1

        return try {
            when (resultState) {
                AsyncState.Empty -> MediatorResult.Error(Throwable("Empty data"))
                is AsyncState.Error -> MediatorResult.Error(Throwable(resultState.error.errorBody))
                is AsyncState.Success -> {
                    val items = resultState.data

                    items.forEach { it.page = nextPage }

                    if (loadType == LoadType.REFRESH) {
                        items.forEach {
                            it.lastUpdatedTimestamp = Clock.System.now().toEpochMilliseconds()
                        }
                    }

                    if (loadType == LoadType.REFRESH && builder.deleteOnRefresh) {
                        if (builder.deleteAll == null)
                            throw NetFlowException("You must implement 'deleteAll()' function if 'deleteOnRefresh' is true!")

                        builder.deleteAll?.invoke()
                    }

                    if (builder.insertAll == null)
                        throw NetFlowException("You must implement 'insertAll()' function!")

                    builder.insertAll?.invoke(items)

                    MediatorResult.Success(items.isEmpty())
                }
            }
        } catch (e: Exception) {
            MediatorResult.Error(e)
        }
    }
}
