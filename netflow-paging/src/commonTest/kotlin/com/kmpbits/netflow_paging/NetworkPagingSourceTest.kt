package com.kmpbits.netflow_paging

import androidx.paging.PagingConfig
import androidx.paging.PagingSource
import com.kmpbits.netflow_core.enums.ErrorResponseType
import com.kmpbits.netflow_core.response.ErrorResponse
import com.kmpbits.netflow_core.states.AsyncState
import com.kmpbits.netflow_paging.builder.PagingBuilder
import com.kmpbits.netflow_paging.model.PagingModel
import com.kmpbits.netflow_paging.source.NetworkPagingSource
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull

private data class ArticleDto(val id: Int, val title: String) : PagingModel()

private data class ArticleModel(val id: Int, val headline: String)

private fun refreshParams(loadSize: Int = 20) =
    PagingSource.LoadParams.Refresh<Int>(key = null, loadSize = loadSize, placeholdersEnabled = false)

private fun appendParams(key: Int, loadSize: Int = 20) =
    PagingSource.LoadParams.Append(key = key, loadSize = loadSize, placeholdersEnabled = false)

class NetworkPagingSourceTest {

    private fun articlesOf(count: Int, page: Int = 1) =
        List(count) { ArticleDto(id = it + 1 + (page - 1) * count, title = "Article ${it + 1}") }

    private fun builderOf(pageSize: Int = 20) = PagingBuilder<ArticleDto, ArticleDto>().apply {
        defaultPageSize = pageSize
        onlyApiCall = true
    }

    // region load — success

    @Test
    fun `load with null key starts at page 1`() = runTest {
        var loadedPage = -1
        val source = NetworkPagingSource(builderOf()) { page ->
            loadedPage = page
            AsyncState.Success(articlesOf(5))
        }

        source.load(refreshParams())

        assertEquals(1, loadedPage)
    }

    @Test
    fun `load returns LoadResult Page with correct items on success`() = runTest {
        val items = articlesOf(3)
        val source = NetworkPagingSource(builderOf()) { AsyncState.Success(items) }

        val result = source.load(refreshParams())

        assertIs<PagingSource.LoadResult.Page<Int, ArticleDto>>(result)
        assertEquals(3, result.data.size)
        assertEquals(items[0].id, result.data[0].id)
    }

    @Test
    fun `load sets prevKey to null`() = runTest {
        val source = NetworkPagingSource(builderOf()) { AsyncState.Success(articlesOf(5)) }

        val result = source.load(refreshParams()) as PagingSource.LoadResult.Page

        assertNull(result.prevKey)
    }

    @Test
    fun `load sets nextKey when page is full`() = runTest {
        val source = NetworkPagingSource(builderOf(pageSize = 5)) { AsyncState.Success(articlesOf(5)) }

        val result = source.load(refreshParams(loadSize = 5)) as PagingSource.LoadResult.Page

        assertEquals(2, result.nextKey)
    }

    @Test
    fun `load sets nextKey to null when page is partial - end of data`() = runTest {
        val source = NetworkPagingSource(builderOf(pageSize = 20)) { AsyncState.Success(articlesOf(3)) }

        val result = source.load(refreshParams()) as PagingSource.LoadResult.Page

        assertNull(result.nextKey)
    }

    @Test
    fun `load uses append key as page number`() = runTest {
        var loadedPage = -1
        val source = NetworkPagingSource(builderOf()) { page ->
            loadedPage = page
            AsyncState.Success(articlesOf(5))
        }

        source.load(appendParams(key = 3))

        assertEquals(3, loadedPage)
    }

    @Test
    fun `load advances items page property to next page`() = runTest {
        val source = NetworkPagingSource(builderOf()) { AsyncState.Success(articlesOf(2)) }

        source.load(refreshParams())

        // getRefreshKey returns lastLoadedPage (1 after first load)
        assertEquals(1, source.getRefreshKey(
            androidx.paging.PagingState(emptyList(), null, PagingConfig(20), 0)
        ))
    }

    // endregion

    // region load — error

    @Test
    fun `load returns LoadResult Error on AsyncState Error`() = runTest {
        val source = NetworkPagingSource(builderOf()) {
            AsyncState.Error(ErrorResponse(500, "Server error", ErrorResponseType.Http))
        }

        val result = source.load(refreshParams())

        assertIs<PagingSource.LoadResult.Error<Int, ArticleDto>>(result)
        assertEquals("Server error", result.throwable.message)
    }

    @Test
    fun `load returns LoadResult Error on exception`() = runTest {
        val source = NetworkPagingSource(builderOf()) {
            throw RuntimeException("Network timeout")
        }

        val result = source.load(refreshParams())

        assertIs<PagingSource.LoadResult.Error<Int, ArticleDto>>(result)
        assertEquals("Network timeout", result.throwable.message)
    }

    // endregion

    // region networkTransform

    @Test
    fun `load applies networkTransform to each item`() = runTest {
        val builder = PagingBuilder<ArticleDto, ArticleModel>().apply {
            defaultPageSize = 20
            onlyApiCall = true
            networkTransform { ArticleModel(it.id, it.title.uppercase()) }
        }

        val source = NetworkPagingSource(builder) { AsyncState.Success(articlesOf(2)) }

        val result = source.load(refreshParams()) as PagingSource.LoadResult.Page

        assertEquals("ARTICLE 1", result.data[0].headline)
        assertEquals("ARTICLE 2", result.data[1].headline)
    }

    // endregion

    // region insertAll callback

    @Test
    fun `load calls insertAll with raw ApiType items on success`() = runTest {
        val inserted = mutableListOf<ArticleDto>()
        val builder = PagingBuilder<ArticleDto, ArticleDto>().apply {
            defaultPageSize = 20
            onlyApiCall = true
            insertAll { items -> inserted.addAll(items) }
        }

        val source = NetworkPagingSource(builder) { AsyncState.Success(articlesOf(3)) }
        source.load(refreshParams())

        assertEquals(3, inserted.size)
        assertEquals(1, inserted[0].id)
    }

    @Test
    fun `load does not call insertAll on error`() = runTest {
        var insertCalled = false
        val builder = PagingBuilder<ArticleDto, ArticleDto>().apply {
            defaultPageSize = 20
            onlyApiCall = true
            insertAll { insertCalled = true }
        }

        val source = NetworkPagingSource(builder) {
            AsyncState.Error(ErrorResponse(500, "error", ErrorResponseType.Http))
        }
        source.load(refreshParams())

        assertEquals(false, insertCalled)
    }

    // endregion

    // region getRefreshKey

    @Test
    fun `getRefreshKey returns last loaded page`() = runTest {
        val source = NetworkPagingSource(builderOf()) { AsyncState.Success(articlesOf(5)) }
        val state = androidx.paging.PagingState<Int, ArticleDto>(emptyList(), null, PagingConfig(20), 0)

        // Before any load, lastLoadedPage is 1
        assertEquals(1, source.getRefreshKey(state))

        source.load(appendParams(key = 4))

        assertEquals(4, source.getRefreshKey(state))
    }

    // endregion
}
