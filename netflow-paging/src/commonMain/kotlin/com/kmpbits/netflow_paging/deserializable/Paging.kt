package com.kmpbits.netflow_paging.deserializable

import androidx.paging.ExperimentalPagingApi
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import com.kmpbits.netflow_core.deserializables.responseListAsync
import com.kmpbits.netflow_core.deserializables.responseWrappedListAsync
import com.kmpbits.netflow_core.exceptions.NetFlowException
import com.kmpbits.netflow_core.request.NetFlowRequest
import com.kmpbits.netflow_core.states.AsyncState
import com.kmpbits.netflow_paging.builder.PagingBuilder
import com.kmpbits.netflow_paging.model.PagingModel
import com.kmpbits.netflow_paging.source.NetworkPagingSource
import com.kmpbits.netflow_paging.source.RemoteAndLocalPagingSource
import kotlinx.coroutines.flow.Flow

/**
 * Deserializes the request into a paginated [Flow] of [PagingData].
 *
 * [ApiType] is the type the API response is deserialized into (must extend [PagingModel]).
 * [DisplayType] is the type exposed in [PagingData] — use this to return domain models
 * directly without an extra [kotlinx.coroutines.flow.Flow.map] on the call site.
 */
@ExperimentalPagingApi
inline fun <reified ApiType : PagingModel, DisplayType : Any> NetFlowRequest.responsePaginated(
    crossinline builder: PagingBuilder<ApiType, DisplayType>.() -> Unit = {}
): Flow<PagingData<DisplayType>> {
    val pagingBuilder = PagingBuilder<ApiType, DisplayType>().also(builder)

    if (pagingBuilder.onlyApiCall.not() && pagingBuilder.itemsDataSource == null)
        throw NetFlowException("Items datasource must not be null!")

    this@responsePaginated.immutableRequestBuilder.preCall?.invoke()

    val apiCall: suspend () -> AsyncState<List<ApiType>> = {
        if (pagingBuilder.wrappedResponse)
            this@responsePaginated.responseWrappedListAsync<ApiType>()
        else
            this@responsePaginated.responseListAsync<ApiType>()
    }

    return if (pagingBuilder.onlyApiCall) {
        Pager(
            config = PagingConfig(pagingBuilder.defaultPageSize),
            pagingSourceFactory = {
                NetworkPagingSource(pagingBuilder) {
                    updateUrlPage(pagingBuilder.pageQueryName, it)
                    apiCall()
                }
            }
        ).flow
    } else {
        Pager(
            config = PagingConfig(pagingBuilder.defaultPageSize),
            remoteMediator = RemoteAndLocalPagingSource(pagingBuilder) {
                updateUrlPage(pagingBuilder.pageQueryName, it)
                apiCall()
            },
            pagingSourceFactory = pagingBuilder.itemsDataSource!!
        ).flow
    }
}

@PublishedApi
internal fun NetFlowRequest.updateUrlPage(pageQueryName: String, page: Int) {
    immutableRequestBuilder.updateParameter(pageQueryName, page)
    updateUrl()
}
