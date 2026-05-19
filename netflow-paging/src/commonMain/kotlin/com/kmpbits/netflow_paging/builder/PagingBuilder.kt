package com.kmpbits.netflow_paging.builder

import androidx.paging.PagingSource
import com.kmpbits.netflow_core.annotations.NetFlowMarker
import com.kmpbits.netflow_paging.model.PagingModel
import com.kmpbits.netflow_paging.source.MappedPagingSource
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours

@NetFlowMarker
class PagingBuilder<T : PagingModel> @PublishedApi internal constructor() {

    /**
     * The page size to load at once from [androidx.paging.PagingSource].
     *
     * Default is 10.
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
    internal var itemsDataSource: (() -> PagingSource<Int, T>)? = null

    @PublishedApi
    internal var lastUpdatedTimestamp: (suspend () -> Long?)? = null

    internal var deleteAll: ( suspend () -> Unit)? = null
    internal var insertAll: ( suspend (items: List<T>) -> Unit)? = null

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
    fun insertAll(onSuccess: suspend (items: List<T>) -> Unit) {
        this.insertAll = onSuccess
    }

    /**
     * Call this function to insert all the data in the local data source with a transform to map
     * from the api model type [T] to the local entity type [E] before inserting.
     *
     * <b>Warning:</b> this function is mandatory if [onlyApiCall] is false.
     */
    fun <E> insertAll(transform: (T) -> E, onSuccess: suspend (items: List<E>) -> Unit) {
        this.insertAll = { items -> onSuccess(items.map(transform)) }
    }

    /**
     * Call this function to get items from local data source.
     *
     * <b>Warning:</b> this function is mandatory if [onlyApiCall] is false.
     */
    fun localSource(pagingSource: () -> PagingSource<Int, T>) {
        this.itemsDataSource = pagingSource
    }

    /**
     * Call this function to get items from local data source with a transform to map from
     * the local entity type [E] to the paging model type [T].
     *
     * <b>Warning:</b> this function is mandatory if [onlyApiCall] is false.
     */
    fun <E : Any> localSource(pagingSource: () -> PagingSource<Int, E>, transform: (E) -> T) {
        this.itemsDataSource = { MappedPagingSource(pagingSource(), transform) }
    }

    /**
     * Call this function to get the first item from local data source.
     * <b>Warning:</b> this functions is necessary if you don't want to refresh content when open the app.
     */
    fun firstItemDatabase(itemDatabase: suspend () -> T?) {
        lastUpdatedTimestamp = { itemDatabase()?.lastUpdatedTimestamp }
    }

    /**
     * Call this function to get the first item from local data source when using a custom entity type [E].
     * <b>Warning:</b> this functions is necessary if you don't want to refresh content when open the app.
     */
    fun <E> firstItemDatabase(itemDatabase: suspend () -> E?, timestamp: (E) -> Long?) {
        lastUpdatedTimestamp = { itemDatabase()?.let { timestamp(it) } }
    }
}