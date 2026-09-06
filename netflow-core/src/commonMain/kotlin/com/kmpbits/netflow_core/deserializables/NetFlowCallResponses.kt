package com.kmpbits.netflow_core.deserializables

import com.kmpbits.netflow_core.builders.ResponseBuilder
import com.kmpbits.netflow_core.request.NetFlowCall
import com.kmpbits.netflow_core.states.AsyncState
import com.kmpbits.netflow_core.states.ResultState
import kotlinx.coroutines.flow.Flow

/*
 * Pure-delegation overloads: every `NetFlowRequest.responseX` extension, with a `NetFlowCall`
 * receiver, forwarding to `request.responseX(...)`. Keep in sync with Flow.kt / Async.kt / Response.kt.
 */

// ---------------------------------------------------------------------------------------------------
// Flow
// ---------------------------------------------------------------------------------------------------

inline fun <reified T : Any> NetFlowCall.responseFlow(
    crossinline responseBuilder: ResponseBuilder<T, T>.() -> Unit = {},
): Flow<ResultState<T>> = request.responseFlow(responseBuilder)

inline fun <reified ApiType : Any, DisplayType : Any> NetFlowCall.responseFlow(
    noinline transform: (ApiType) -> DisplayType,
    crossinline responseBuilder: ResponseBuilder<ApiType, DisplayType>.() -> Unit = {},
): Flow<ResultState<DisplayType>> = request.responseFlow(transform, responseBuilder)

inline fun <reified T : Any> NetFlowCall.responseWrappedFlow(
    crossinline responseBuilder: ResponseBuilder<T, T>.() -> Unit = {},
): Flow<ResultState<T>> = request.responseWrappedFlow(responseBuilder)

inline fun <reified ApiType : Any, DisplayType : Any> NetFlowCall.responseWrappedFlow(
    noinline transform: (ApiType) -> DisplayType,
    crossinline responseBuilder: ResponseBuilder<ApiType, DisplayType>.() -> Unit = {},
): Flow<ResultState<DisplayType>> = request.responseWrappedFlow(transform, responseBuilder)

inline fun <reified T : Any> NetFlowCall.responseListFlow(
    crossinline responseBuilder: ResponseBuilder<List<T>, List<T>>.() -> Unit = {},
): Flow<ResultState<List<T>>> = request.responseListFlow(responseBuilder)

inline fun <reified ApiItem : Any, DisplayItem : Any> NetFlowCall.responseListFlow(
    noinline transform: (ApiItem) -> DisplayItem,
    crossinline responseBuilder: ResponseBuilder<List<ApiItem>, List<DisplayItem>>.() -> Unit = {},
): Flow<ResultState<List<DisplayItem>>> = request.responseListFlow(transform, responseBuilder)

inline fun <reified T : Any> NetFlowCall.responseWrappedListFlow(
    crossinline responseBuilder: ResponseBuilder<List<T>, List<T>>.() -> Unit = {},
): Flow<ResultState<List<T>>> = request.responseWrappedListFlow(responseBuilder)

inline fun <reified ApiItem : Any, DisplayItem : Any> NetFlowCall.responseWrappedListFlow(
    noinline transform: (ApiItem) -> DisplayItem,
    crossinline responseBuilder: ResponseBuilder<List<ApiItem>, List<DisplayItem>>.() -> Unit = {},
): Flow<ResultState<List<DisplayItem>>> = request.responseWrappedListFlow(transform, responseBuilder)

// ---------------------------------------------------------------------------------------------------
// Async
// ---------------------------------------------------------------------------------------------------

suspend inline fun <reified T : Any> NetFlowCall.responseAsync(
    crossinline responseBuilder: ResponseBuilder<T, T>.() -> Unit = {},
): AsyncState<T> = request.responseAsync(responseBuilder)

suspend inline fun <reified ApiType : Any, DisplayType : Any> NetFlowCall.responseAsync(
    noinline transform: (ApiType) -> DisplayType,
    crossinline responseBuilder: ResponseBuilder<ApiType, DisplayType>.() -> Unit = {},
): AsyncState<DisplayType> = request.responseAsync(transform, responseBuilder)

suspend inline fun <reified T : Any> NetFlowCall.responseListAsync(
    crossinline responseBuilder: ResponseBuilder<List<T>, List<T>>.() -> Unit = {},
): AsyncState<List<T>> = request.responseListAsync(responseBuilder)

suspend inline fun <reified ApiItem : Any, DisplayItem : Any> NetFlowCall.responseListAsync(
    noinline transform: (ApiItem) -> DisplayItem,
    crossinline responseBuilder: ResponseBuilder<List<ApiItem>, List<DisplayItem>>.() -> Unit = {},
): AsyncState<List<DisplayItem>> = request.responseListAsync(transform, responseBuilder)

suspend inline fun <reified T : Any> NetFlowCall.responseWrappedAsync(
    crossinline responseBuilder: ResponseBuilder<T, T>.() -> Unit = {},
): AsyncState<T> = request.responseWrappedAsync(responseBuilder)

suspend inline fun <reified ApiType : Any, DisplayType : Any> NetFlowCall.responseWrappedAsync(
    noinline transform: (ApiType) -> DisplayType,
    crossinline responseBuilder: ResponseBuilder<ApiType, DisplayType>.() -> Unit = {},
): AsyncState<DisplayType> = request.responseWrappedAsync(transform, responseBuilder)

suspend inline fun <reified T : Any> NetFlowCall.responseWrappedListAsync(
    crossinline responseBuilder: ResponseBuilder<List<T>, List<T>>.() -> Unit = {},
): AsyncState<List<T>> = request.responseWrappedListAsync(responseBuilder)

suspend inline fun <reified ApiItem : Any, DisplayItem : Any> NetFlowCall.responseWrappedListAsync(
    noinline transform: (ApiItem) -> DisplayItem,
    crossinline responseBuilder: ResponseBuilder<List<ApiItem>, List<DisplayItem>>.() -> Unit = {},
): AsyncState<List<DisplayItem>> = request.responseWrappedListAsync(transform, responseBuilder)

// ---------------------------------------------------------------------------------------------------
// Response
// ---------------------------------------------------------------------------------------------------

suspend inline fun <reified T> NetFlowCall.responseToModel(): T = request.responseToModel()
