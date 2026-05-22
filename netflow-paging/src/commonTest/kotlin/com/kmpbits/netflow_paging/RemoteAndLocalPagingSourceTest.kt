package com.kmpbits.netflow_paging

import androidx.paging.ExperimentalPagingApi
import androidx.paging.LoadType
import androidx.paging.PagingConfig
import androidx.paging.PagingSource
import androidx.paging.PagingState
import androidx.paging.RemoteMediator
import com.kmpbits.netflow_core.enums.ErrorResponseType
import com.kmpbits.netflow_core.response.ErrorResponse
import com.kmpbits.netflow_core.states.AsyncState
import com.kmpbits.netflow_paging.builder.PagingBuilder
import com.kmpbits.netflow_paging.model.PagingModel
import com.kmpbits.netflow_paging.source.RemoteAndLocalPagingSource
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

private data class PostDto(val id: Int, val title: String) : PagingModel()

@OptIn(ExperimentalPagingApi::class, ExperimentalTime::class)
class RemoteAndLocalPagingSourceTest {

    private fun emptyState() = PagingState<Int, PostDto>(
        pages = emptyList(),
        anchorPosition = null,
        config = PagingConfig(pageSize = 20),
        leadingPlaceholderCount = 0
    )

    private fun stateWithNextKey(nextKey: Int) = PagingState(
        pages = listOf(
            PagingSource.LoadResult.Page(
                data = listOf(PostDto(1, "Post")),
                prevKey = null,
                nextKey = nextKey
            )
        ),
        anchorPosition = null,
        config = PagingConfig(pageSize = 20),
        leadingPlaceholderCount = 0
    )

    private fun baseBuilder(
        deleteOnRefresh: Boolean = true,
        refresh: Boolean = false
    ) = PagingBuilder<PostDto, PostDto>().apply {
        this.deleteOnRefresh = deleteOnRefresh
        this.refresh = refresh
        deleteAll { }
        insertAll { }
    }

    private fun postsOf(count: Int) = List(count) { PostDto(it + 1, "Post ${it + 1}") }

    // region initialize

    @Test
    fun `initialize returns LAUNCH_INITIAL_REFRESH when no timestamp`() = runTest {
        val mediator = RemoteAndLocalPagingSource(baseBuilder()) { AsyncState.Success(emptyList()) }

        val action = mediator.initialize()

        assertEquals(RemoteMediator.InitializeAction.LAUNCH_INITIAL_REFRESH, action)
    }

    @Test
    fun `initialize returns SKIP_INITIAL_REFRESH when timestamp is within cacheTimeout`() = runTest {
        val recentTimestamp = Clock.System.now().toEpochMilliseconds() - 1_000L
        val builder = baseBuilder().apply {
            firstItemDatabase { PostDto(1, "Post").apply { lastUpdatedTimestamp = recentTimestamp } }
        }
        val mediator = RemoteAndLocalPagingSource(builder) { AsyncState.Success(emptyList()) }

        val action = mediator.initialize()

        assertEquals(RemoteMediator.InitializeAction.SKIP_INITIAL_REFRESH, action)
    }

    @Test
    fun `initialize returns LAUNCH_INITIAL_REFRESH when timestamp is stale`() = runTest {
        val staleTimestamp = Clock.System.now().toEpochMilliseconds() - 2 * 60 * 60 * 1_000L // 2 hours ago
        val builder = baseBuilder().apply {
            firstItemDatabase { PostDto(1, "Post").apply { lastUpdatedTimestamp = staleTimestamp } }
        }
        val mediator = RemoteAndLocalPagingSource(builder) { AsyncState.Success(emptyList()) }

        val action = mediator.initialize()

        assertEquals(RemoteMediator.InitializeAction.LAUNCH_INITIAL_REFRESH, action)
    }

    @Test
    fun `initialize returns LAUNCH_INITIAL_REFRESH when refresh is true even if timestamp is fresh`() = runTest {
        val recentTimestamp = Clock.System.now().toEpochMilliseconds() - 1_000L
        val builder = baseBuilder(refresh = true).apply {
            firstItemDatabase { PostDto(1, "Post").apply { lastUpdatedTimestamp = recentTimestamp } }
        }
        val mediator = RemoteAndLocalPagingSource(builder) { AsyncState.Success(emptyList()) }

        val action = mediator.initialize()

        assertEquals(RemoteMediator.InitializeAction.LAUNCH_INITIAL_REFRESH, action)
    }

    // endregion

    // region PREPEND

    @Test
    fun `load PREPEND always returns Success with endOfPaginationReached true`() = runTest {
        val mediator = RemoteAndLocalPagingSource(baseBuilder()) { AsyncState.Success(postsOf(5)) }

        val result = mediator.load(LoadType.PREPEND, emptyState())

        assertIs<RemoteMediator.MediatorResult.Success>(result)
        assertEquals(true, result.endOfPaginationReached)
    }

    @Test
    fun `load PREPEND does not make any API call`() = runTest {
        var apiCalled = false
        val mediator = RemoteAndLocalPagingSource(baseBuilder()) {
            apiCalled = true
            AsyncState.Success(postsOf(1))
        }

        mediator.load(LoadType.PREPEND, emptyState())

        assertEquals(false, apiCalled)
    }

    // endregion

    // region REFRESH success

    @Test
    fun `load REFRESH calls deleteAll when deleteOnRefresh is true`() = runTest {
        var deleteCalled = false
        val builder = PagingBuilder<PostDto, PostDto>().apply {
            deleteOnRefresh = true
            deleteAll { deleteCalled = true }
            insertAll { }
        }
        val mediator = RemoteAndLocalPagingSource(builder) { AsyncState.Success(postsOf(3)) }

        mediator.load(LoadType.REFRESH, emptyState())

        assertEquals(true, deleteCalled)
    }

    @Test
    fun `load REFRESH does not call deleteAll when deleteOnRefresh is false`() = runTest {
        var deleteCalled = false
        val builder = PagingBuilder<PostDto, PostDto>().apply {
            deleteOnRefresh = false
            deleteAll { deleteCalled = true }
            insertAll { }
        }
        val mediator = RemoteAndLocalPagingSource(builder) { AsyncState.Success(postsOf(3)) }

        mediator.load(LoadType.REFRESH, emptyState())

        assertEquals(false, deleteCalled)
    }

    @Test
    fun `load REFRESH calls insertAll with items from API`() = runTest {
        val inserted = mutableListOf<PostDto>()
        val builder = PagingBuilder<PostDto, PostDto>().apply {
            deleteAll { }
            insertAll { items -> inserted.addAll(items) }
        }
        val mediator = RemoteAndLocalPagingSource(builder) { AsyncState.Success(postsOf(3)) }

        mediator.load(LoadType.REFRESH, emptyState())

        assertEquals(3, inserted.size)
        assertEquals(1, inserted[0].id)
    }

    @Test
    fun `load REFRESH returns Success with endOfPaginationReached true when empty list`() = runTest {
        val mediator = RemoteAndLocalPagingSource(baseBuilder()) { AsyncState.Success(emptyList()) }

        val result = mediator.load(LoadType.REFRESH, emptyState())

        assertIs<RemoteMediator.MediatorResult.Success>(result)
        assertEquals(true, result.endOfPaginationReached)
    }

    @Test
    fun `load REFRESH returns Success with endOfPaginationReached false when items present`() = runTest {
        val mediator = RemoteAndLocalPagingSource(baseBuilder()) { AsyncState.Success(postsOf(5)) }

        val result = mediator.load(LoadType.REFRESH, emptyState())

        assertIs<RemoteMediator.MediatorResult.Success>(result)
        assertEquals(false, result.endOfPaginationReached)
    }

    @Test
    fun `load REFRESH sets lastUpdatedTimestamp on items`() = runTest {
        val capturedItems = mutableListOf<PostDto>()
        val builder = PagingBuilder<PostDto, PostDto>().apply {
            deleteAll { }
            insertAll { items -> capturedItems.addAll(items) }
        }
        val before = Clock.System.now().toEpochMilliseconds()
        val mediator = RemoteAndLocalPagingSource(builder) { AsyncState.Success(postsOf(2)) }

        mediator.load(LoadType.REFRESH, emptyState())

        capturedItems.forEach { post ->
            val ts = post.lastUpdatedTimestamp
            assertEquals(true, ts != null && ts >= before, "Expected timestamp >= $before, got $ts")
        }
    }

    // endregion

    // region REFRESH error

    @Test
    fun `load REFRESH returns MediatorResult Error on AsyncState Error`() = runTest {
        val mediator = RemoteAndLocalPagingSource(baseBuilder()) {
            AsyncState.Error(ErrorResponse(500, "Server down", ErrorResponseType.Http))
        }

        val result = mediator.load(LoadType.REFRESH, emptyState())

        assertIs<RemoteMediator.MediatorResult.Error>(result)
        assertEquals("Server down", result.throwable.message)
    }

    @Test
    fun `load REFRESH returns MediatorResult Error when deleteAll is null and deleteOnRefresh is true`() = runTest {
        val builder = PagingBuilder<PostDto, PostDto>().apply {
            deleteOnRefresh = true
            // deleteAll intentionally not set
            insertAll { }
        }
        val mediator = RemoteAndLocalPagingSource(builder) { AsyncState.Success(postsOf(1)) }

        val result = mediator.load(LoadType.REFRESH, emptyState())

        assertIs<RemoteMediator.MediatorResult.Error>(result)
    }

    @Test
    fun `load REFRESH returns MediatorResult Error when insertAll is null`() = runTest {
        val builder = PagingBuilder<PostDto, PostDto>().apply {
            deleteAll { }
            // insertAll intentionally not set
        }
        val mediator = RemoteAndLocalPagingSource(builder) { AsyncState.Success(postsOf(1)) }

        val result = mediator.load(LoadType.REFRESH, emptyState())

        assertIs<RemoteMediator.MediatorResult.Error>(result)
    }

    // endregion

    // region APPEND

    @Test
    fun `load APPEND returns Success with endOfPaginationReached false when no pages loaded`() = runTest {
        val mediator = RemoteAndLocalPagingSource(baseBuilder()) { AsyncState.Success(postsOf(5)) }

        val result = mediator.load(LoadType.APPEND, emptyState())

        assertIs<RemoteMediator.MediatorResult.Success>(result)
        assertEquals(false, result.endOfPaginationReached)
    }

    @Test
    fun `load APPEND uses nextKey from last page`() = runTest {
        var loadedPage = -1
        val mediator = RemoteAndLocalPagingSource(baseBuilder()) { page ->
            loadedPage = page
            AsyncState.Success(postsOf(5))
        }

        mediator.load(LoadType.APPEND, stateWithNextKey(nextKey = 3))

        assertEquals(3, loadedPage)
    }

    @Test
    fun `load APPEND calls insertAll without calling deleteAll`() = runTest {
        var deleteCalled = false
        val inserted = mutableListOf<PostDto>()
        val builder = PagingBuilder<PostDto, PostDto>().apply {
            deleteAll { deleteCalled = true }
            insertAll { items -> inserted.addAll(items) }
        }
        val mediator = RemoteAndLocalPagingSource(builder) { AsyncState.Success(postsOf(2)) }

        mediator.load(LoadType.APPEND, stateWithNextKey(nextKey = 2))

        assertEquals(false, deleteCalled)
        assertEquals(2, inserted.size)
    }

    // endregion
}
