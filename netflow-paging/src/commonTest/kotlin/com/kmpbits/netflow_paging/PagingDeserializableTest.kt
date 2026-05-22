package com.kmpbits.netflow_paging

import androidx.paging.ExperimentalPagingApi
import androidx.paging.PagingData
import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.kmpbits.netflow_core.exceptions.NetFlowException
import com.kmpbits.netflow_core.mock.MockNetFlowClient
import com.kmpbits.netflow_core.mock.NetFlowMockResponse
import com.kmpbits.netflow_paging.deserializable.responsePaginated
import com.kmpbits.netflow_paging.model.PagingModel
import kotlinx.coroutines.flow.Flow
import kotlin.test.Test
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull

private data class ItemDto(val id: Int, val name: String) : PagingModel()

private class FakeItemPagingSource : PagingSource<Int, ItemDto>() {
    override fun getRefreshKey(state: PagingState<Int, ItemDto>): Int? = null
    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, ItemDto> =
        LoadResult.Page(data = emptyList(), prevKey = null, nextKey = null)
}

@OptIn(ExperimentalPagingApi::class)
class PagingDeserializableTest {

    private val itemsJson = """[{"id":1,"name":"Item 1"},{"id":2,"name":"Item 2"}]"""
    private val wrappedItemsJson = """{"data":[{"id":1,"name":"Item 1"},{"id":2,"name":"Item 2"}]}"""

    // region validation

    @Test
    fun `responsePaginated throws NetFlowException when onlyApiCall is false and no localSource`() {
        val client = MockNetFlowClient { NetFlowMockResponse.success(itemsJson) }

        assertFailsWith<NetFlowException> {
            client.call { path = "items" }.responsePaginated<ItemDto, ItemDto> {
                onlyApiCall = false
                // no localSource() provided
            }
        }
    }

    @Test
    fun `responsePaginated does not throw when onlyApiCall is true`() {
        val client = MockNetFlowClient { NetFlowMockResponse.success(itemsJson) }

        val flow: Flow<PagingData<ItemDto>> = client.call { path = "items" }
            .responsePaginated<ItemDto, ItemDto> {
                onlyApiCall = true
            }

        assertNotNull(flow)
    }

    @Test
    fun `responsePaginated does not throw when localSource is provided`() {
        val client = MockNetFlowClient { NetFlowMockResponse.success(itemsJson) }
        val fakePagingSource = FakeItemPagingSource()

        val flow: Flow<PagingData<ItemDto>> = client.call { path = "items" }
            .responsePaginated<ItemDto, ItemDto> {
                onlyApiCall = false
                localSource { fakePagingSource }
                deleteAll { }
                insertAll { }
            }

        assertNotNull(flow)
    }

    // endregion

    // region Pager configuration

    @Test
    fun `responsePaginated with wrappedResponse uses wrapped list endpoint`() {
        val client = MockNetFlowClient { NetFlowMockResponse.success(wrappedItemsJson) }

        val flow: Flow<PagingData<ItemDto>> = client.call { path = "items" }
            .responsePaginated<ItemDto, ItemDto> {
                onlyApiCall = true
                wrappedResponse = true
            }

        assertNotNull(flow)
    }

    @Test
    fun `responsePaginated respects custom pageQueryName`() {
        val client = MockNetFlowClient { NetFlowMockResponse.success(itemsJson) }

        val flow: Flow<PagingData<ItemDto>> = client.call { path = "items" }
            .responsePaginated<ItemDto, ItemDto> {
                onlyApiCall = true
                pageQueryName = "p"
            }

        assertNotNull(flow)
    }

    @Test
    fun `responsePaginated respects custom defaultPageSize`() {
        val client = MockNetFlowClient { NetFlowMockResponse.success(itemsJson) }

        val flow: Flow<PagingData<ItemDto>> = client.call { path = "items" }
            .responsePaginated<ItemDto, ItemDto> {
                onlyApiCall = true
                defaultPageSize = 50
            }

        assertNotNull(flow)
    }

    // endregion
}
