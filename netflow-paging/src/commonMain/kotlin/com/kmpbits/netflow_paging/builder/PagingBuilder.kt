package com.kmpbits.netflow_paging.builder

import androidx.paging.PagingSource
import com.kmpbits.netflow_core.annotations.NetFlowMarker
import com.kmpbits.netflow_paging.model.PagingModel
import com.kmpbits.netflow_paging.source.LongKeyedPagingSourceWrapper
import com.kmpbits.netflow_paging.source.MappedPagingSource
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours

@NetFlowMarker
class PagingBuilder<ApiType : PagingModel, DisplayType : Any> @PublishedApi internal constructor() {

    /**
     * The page size to load at once from [androidx.paging.PagingSource].
     *
     * Default is 20.
     */
    var defaultPageSize: Int = 20

    /**
     * Whether or not delete all the items when [LoadType] is [LoadType.REFRESH] state.
     *
     * Default is true.
     */
    var deleteOnRefresh: Boolean = true

    /**
     * Whether if the [androidx.paging.PagingSource] comes only from api source.
     *
     * If true, it's calling the [NetworkPagingSource], otherwise, [RemoteAndLocalPagingSource].
     *
     * Default is false.
     */
    var onlyApiCall: Boolean = false

    /**
     * The name of the page query parameter on the request.
     */
    var pageQueryName = "page"

    /**
     * Whether the API response is wrapped in a data json object.
     *
     * Set to true when the API returns `{ "data": [...] }` instead of a plain array `[...]`.
     *
     * Default is false.
     */
    var wrappedResponse: Boolean = false

    /**
     * The time for the [RemoteAndLocalPagingSource] to refresh the data.
     *
     * After the time is reached, [RemoteAndLocalPagingSource] will call the api again and replace all the data.
     *
     * This has no effect if the [refresh] flag is true.
     */
    var cacheTimeout: Duration = 1.hours

    /**
     * Force [RemoteAndLocalPagingSource] to refresh when starting.
     *
     * This will update the updated timestamp on [PagingModel].
     *
     * When it is false, the [RemoteAndLocalPagingSource] will update when the last updated timestamp reach the [cacheTimeout].
     *
     * Default is false
     */
    var refresh: Boolean = false

    @PublishedApi
    internal var itemsDataSource: (() -> PagingSource<Int, DisplayType>)? = null

    @PublishedApi
    internal var lastUpdatedTimestamp: (suspend () -> Long?)? = null

    @PublishedApi
    internal var networkTransform: ((ApiType) -> DisplayType)? = null

    internal var deleteAll: (suspend () -> Unit)? = null
    internal var insertAll: (suspend (items: List<ApiType>) -> Unit)? = null

    /**
     * Call this to delete all data from local data source.
     *
     * <b>Warning:</b> this function is mandatory if [onlyApiCall] is false.
     */
    fun deleteAll(deleteAll: suspend () -> Unit) {
        this.deleteAll = deleteAll
    }

    /**
     * Call this function to insert all the data in the local data source that comes from api.
     *
     * <b>Warning:</b> this function is mandatory if [onlyApiCall] is false.
     */
    fun insertAll(onSuccess: suspend (items: List<ApiType>) -> Unit) {
        this.insertAll = onSuccess
    }

    /**
     * Call this function to insert all the data in the local data source with a transform to map
     * from the api model type [ApiType] to the local entity type [E] before inserting.
     *
     * <b>Warning:</b> this function is mandatory if [onlyApiCall] is false.
     */
    fun <E> insertAll(transform: (ApiType) -> E, onSuccess: suspend (items: List<E>) -> Unit) {
        this.insertAll = { items -> onSuccess(items.map(transform)) }
    }

    /**
     * Call this function to get items from local data source.
     *
     * <b>Warning:</b> this function is mandatory if [onlyApiCall] is false.
     */
    fun localSource(pagingSource: () -> PagingSource<Int, DisplayType>) {
        this.itemsDataSource = pagingSource
    }

    /**
     * Call this function to get items from local data source with a transform to map from
     * the local entity type [E] to the display type [DisplayType].
     *
     * <b>Warning:</b> this function is mandatory if [onlyApiCall] is false.
     */
    fun <E : Any> localSource(pagingSource: () -> PagingSource<Int, E>, transform: (E) -> DisplayType) {
        this.itemsDataSource = { MappedPagingSource(pagingSource(), transform) }
    }

    /**
     * Variant of [localSource] for [PagingSource] implementations that use [Long] keys (e.g. SQLDelight's QueryPagingSource).
     * Keys are automatically bridged to [Int] internally.
     */
    fun <E : Any> localSourceLong(pagingSource: () -> PagingSource<Long, E>, transform: (E) -> DisplayType) {
        this.itemsDataSource = { LongKeyedPagingSourceWrapper(pagingSource(), transform) }
    }

    /**
     * Call this function to map the api response type [ApiType] to [DisplayType] when using [onlyApiCall].
     *
     * <b>Warning:</b> this function is mandatory if [onlyApiCall] is true and [ApiType] != [DisplayType].
     */
    fun networkTransform(transform: (ApiType) -> DisplayType) {
        this.networkTransform = transform
    }

    /**
     * Call this function to get the first item from local data source.
     * <b>Warning:</b> this function is necessary if you don't want to refresh content when open the app.
     */
    fun firstItemDatabase(itemDatabase: suspend () -> ApiType?) {
        lastUpdatedTimestamp = { itemDatabase()?.lastUpdatedTimestamp }
    }

    /**
     * Call this function to get the first item from local data source when using a custom entity type [E].
     * <b>Warning:</b> this function is necessary if you don't want to refresh content when open the app.
     */
    fun <E> firstItemDatabase(itemDatabase: suspend () -> E?, timestamp: (E) -> Long?) {
        lastUpdatedTimestamp = { itemDatabase()?.let { timestamp(it) } }
    }
}
