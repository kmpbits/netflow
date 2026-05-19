package com.kmpbits.netflow_paging.source

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.kmpbits.netflow_core.envelope.EnvelopeList
import com.kmpbits.netflow_core.states.AsyncState
import com.kmpbits.netflow_paging.PagingBuilder
import com.kmpbits.netflow_paging.model.PagingModel

@PublishedApi
internal class NetworkPagingSource<T : PagingModel>(
    private val builder: PagingBuilder<T>,
    private val doApiCall: suspend (page: Int) -> AsyncState<EnvelopeList<T>>
) : PagingSource<Int, T>() {

    override fun getRefreshKey(state: PagingState<Int, T>): Int? {
        return state.lastItemOrNull()?.page
    }

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, T> {
        val page = params.key ?: 1

        return try {
            val resultState = doApiCall(page)
            val nextPage = page + 1

            return when(resultState) {
                AsyncState.Empty -> LoadResult.Error(Throwable("Empty data"))
                is AsyncState.Error -> LoadResult.Error(Throwable(resultState.error.errorBody))
                is AsyncState.Success -> {
                    val envelopeList = resultState.data
                    envelopeList.data.forEach {
                        it.page = nextPage
                    }

                    builder.insertAll?.let {
                        it(envelopeList.data)
                    }

                    LoadResult.Page(
                        data = envelopeList.data,
                        prevKey = null,
                        nextKey = if (envelopeList.data.size < builder.defaultPageSize)
                            null
                        else
                            nextPage
                    )
                }
            }

        } catch (e: Exception) {
            LoadResult.Error(e)
        }
    }
}