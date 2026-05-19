package com.kmpbits.netflow_paging.deserializable

import androidx.paging.ExperimentalPagingApi
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import com.kmpbits.netflow_core.deserializables.responseAsync
import com.kmpbits.netflow_core.exceptions.NetFlowException
import com.kmpbits.netflow_core.request.NetFlowRequest
import com.kmpbits.netflow_paging.PagingBuilder
import com.kmpbits.netflow_paging.model.PagingModel
import com.kmpbits.netflow_paging.source.NetworkPagingSource
import com.kmpbits.netflow_paging.source.RemoteAndLocalPagingSource
import kotlinx.coroutines.flow.Flow

@ExperimentalPagingApi
inline fun <reified T : PagingModel> NetFlowRequest.responsePaginated(
    crossinline builder: PagingBuilder<T>. () -> Unit = {}
): Flow<PagingData<T>> {
    val pagingBuilder = PagingBuilder<T>().also(builder)

    if (pagingBuilder.onlyApiCall.not() && pagingBuilder.itemsDataSource == null)
        throw NetFlowException("Items datasource must not be null!")

    this@responsePaginated.immutableRequestBuilder.preCall?.invoke()

    return if (pagingBuilder.onlyApiCall) {
        Pager(
            config = PagingConfig(pagingBuilder.defaultPageSize),
            pagingSourceFactory = {
                NetworkPagingSource(
                    pagingBuilder
                ) {
                    updateUrlPage(pagingBuilder.pageQueryName, it)
                    this@responsePaginated.responseAsync()
                }
            }
        ).flow
    } else {
        Pager(
            config = PagingConfig(pagingBuilder.defaultPageSize),
            remoteMediator = RemoteAndLocalPagingSource(
                pagingBuilder
            ) {
                updateUrlPage(pagingBuilder.pageQueryName, it)
                this@responsePaginated.responseAsync()
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