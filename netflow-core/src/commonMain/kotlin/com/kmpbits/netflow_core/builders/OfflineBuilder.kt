package com.kmpbits.netflow_core.builders

import com.kmpbits.netflow_core.annotations.NetFlowMarker
import kotlinx.coroutines.flow.Flow

@NetFlowMarker
class OfflineBuilder<T> internal constructor() {

    /**
     * Whether or not the request should only call a local data source.
     *
     * If it's true, the data source will come from both local and remote
     *
     * Default is false.
     */
    var onlyLocalCall: Boolean = false

    @PublishedApi
    internal var call: (suspend () -> T?)? = null

    @PublishedApi
    internal var callFlow: (() -> Flow<T?>)? = null

    /**
     * Call this function to handle the local call. It can be from any local database
     *
     * @param call The query from the local data source. It expects to receive an object of type [T].
     * The call is suspend, so you don't need to handle the thread change
     */
    fun call(call: suspend () -> T?) {
        this.call = call
    }

    /**
     * Call this function to observe the local data source.
     *
     * @param callFlow The query from local data source.
     * It expects to be of type [Flow] and it will emit values when
     * the database is changed (Depending on the database that is being used)
     */
    fun observe(callFlow: () -> Flow<T?>) {
        this.callFlow = callFlow
    }
}