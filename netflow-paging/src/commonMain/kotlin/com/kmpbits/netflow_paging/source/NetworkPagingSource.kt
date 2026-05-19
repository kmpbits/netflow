package com.kmpbits.netflow_paging.source

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.kmpbits.netflow_core.states.AsyncState
import com.kmpbits.netflow_paging.builder.PagingBuilder
import com.kmpbits.netflow_paging.model.PagingModel

@PublishedApi
internal class NetworkPagingSource<ApiType : PagingModel, DisplayType : Any>(
    private val builder: PagingBuilder<ApiType, DisplayType>,
    private val doApiCall: suspend (page: Int) -> AsyncState<List<ApiType>>
) : PagingSource<Int, DisplayType>() {

    private var lastLoadedPage: Int = 1

    override fun getRefreshKey(state: PagingState<Int, DisplayType>): Int? = lastLoadedPage

    @Suppress("UNCHECKED_CAST")
    private fun ApiType.toDisplayType(): DisplayType =
        builder.networkTransform?.invoke(this) ?: this as DisplayType

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, DisplayType> {
        val page = params.key ?: 1

        return try {
            val resultState = doApiCall(page)
            val nextPage = page + 1

            return when (resultState) {
                AsyncState.Empty -> LoadResult.Error(Throwable("Empty data"))
                is AsyncState.Error -> LoadResult.Error(Throwable(resultState.error.errorBody))
                is AsyncState.Success -> {
                    val items = resultState.data
                    items.forEach { it.page = nextPage }

                    builder.insertAll?.let { it(items) }

                    lastLoadedPage = page

                    LoadResult.Page(
                        data = items.map { it.toDisplayType() },
                        prevKey = null,
                        nextKey = if (items.size < builder.defaultPageSize) null else nextPage
                    )
                }
            }

        } catch (e: Exception) {
            LoadResult.Error(e)
        }
    }
}
