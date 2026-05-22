package com.kmpbits.netflow_paging.source

import androidx.paging.PagingSource
import androidx.paging.PagingState
@PublishedApi
internal class MappedPagingSource<E : Any, T : Any>(
    private val source: PagingSource<Int, E>,
    private val transform: (E) -> T
) : PagingSource<Int, T>() {

    init {
        source.registerInvalidatedCallback { invalidate() }
    }

    override fun getRefreshKey(state: PagingState<Int, T>): Int? {
        return state.anchorPosition?.let { anchor ->
            state.closestPageToPosition(anchor)?.prevKey?.plus(1)
                ?: state.closestPageToPosition(anchor)?.nextKey?.minus(1)
        }
    }

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, T> {
        return when (val result = source.load(params)) {
            is LoadResult.Error -> LoadResult.Error(result.throwable)
            is LoadResult.Invalid -> LoadResult.Invalid()
            is LoadResult.Page -> LoadResult.Page(
                data = result.data.map(transform),
                prevKey = result.prevKey,
                nextKey = result.nextKey
            )
        }
    }
}
