package com.kmpbits.netflow_paging.deserializable

import androidx.paging.ExperimentalPagingApi
import androidx.paging.PagingData
import com.kmpbits.netflow_core.request.NetFlowCall
import com.kmpbits.netflow_paging.builder.PagingBuilder
import com.kmpbits.netflow_paging.model.PagingModel
import kotlinx.coroutines.flow.Flow

/**
 * [responsePaginated] for a [NetFlowCall] — forwards to the [com.kmpbits.netflow_core.request.NetFlowRequest]
 * overload. See that function for the semantics.
 */
@ExperimentalPagingApi
inline fun <reified ApiType : PagingModel, DisplayType : Any> NetFlowCall.responsePaginated(
    crossinline builder: PagingBuilder<ApiType, DisplayType>.() -> Unit = {},
): Flow<PagingData<DisplayType>> = request.responsePaginated(builder)
