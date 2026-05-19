package com.kmpbits.sample.android.data.source

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.kmpbits.sample.android.data.database.dao.TodoDao
import com.kmpbits.sample.android.data.database.entity.TodoEntity

class LocalPagingSource(
    private val todoDao: TodoDao
) : PagingSource<Int, TodoEntity>() {

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, TodoEntity> {
        return try {
            val page = params.key ?: 0
            val limit = params.loadSize
            val offset = page * limit

            val todos = todoDao.getTodosPaged(limit, offset)

            LoadResult.Page(
                data = todos,
                prevKey = if (page == 0) null else page - 1,
                nextKey = if (todos.size < limit) null else page + 1
            )
        } catch (e: Exception) {
            LoadResult.Error(e)
        }
    }

    override fun getRefreshKey(state: PagingState<Int, TodoEntity>): Int? {
        return state.anchorPosition?.let { anchorPosition ->
            state.closestPageToPosition(anchorPosition)?.prevKey?.plus(1)
                ?: state.closestPageToPosition(anchorPosition)?.nextKey?.minus(1)
        }
    }
}
