package com.kmpbits.sample.android.data.source

import androidx.paging.PagingSource
import androidx.paging.PagingState
import app.cash.sqldelight.Query
import com.kmpbits.sample.android.data.database.AppDatabase
import com.kmpbits.sample.android.data.database.TodoEntity

class TodoPagingSource(
    private val database: AppDatabase
) : PagingSource<Int, TodoEntity>() {

    private val query = database.todoQueries.getTodos()

    private val listener = object : Query.Listener {
        override fun queryResultsChanged() {
            invalidate()
            query.removeListener(this)
        }
    }

    init {
        query.addListener(listener)
    }

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, TodoEntity> {
        return try {
            val page = params.key ?: 0
            val limit = params.loadSize
            val offset = (page * limit).toLong()

            val todos = database.todoQueries
                .getTodosPaged(limit = limit.toLong(), offset = offset)
                .executeAsList()

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
        return state.anchorPosition?.let { anchor ->
            state.closestPageToPosition(anchor)?.prevKey?.plus(1)
                ?: state.closestPageToPosition(anchor)?.nextKey?.minus(1)
        }
    }
}
