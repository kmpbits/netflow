package com.kmpbits.netflow_core.builders

import com.kmpbits.netflow_core.annotations.NetFlowMarker

@NetFlowMarker
class ResponseBuilder<T> @PublishedApi internal constructor(){

    @PublishedApi
    internal var offlineBuilder: OfflineBuilder<T>? = null

    @PublishedApi
    internal var post: (suspend () -> Unit)? = null

    @PublishedApi
    internal var onNetworkSuccess: (suspend (model: T) -> Unit)? = null

    /**
     * Call this function to access the [OfflineBuilder] and handle local calls.
     */
    fun local(builder: OfflineBuilder<T>. () -> Unit) {
        offlineBuilder = OfflineBuilder<T>().also(builder)
    }

    /**
     * Call this function to handle the success response and do anything with the data (ex: Inserting into database)
     * The lambda is invoked when the response is success.
     *
     * @param onSuccess The lambda with a success data response of type [T] and it is suspended.
     */
    fun onNetworkSuccess(onSuccess: suspend (data: T) -> Unit) {
        this.onNetworkSuccess = onSuccess
    }

    /**
     * Call this function to handle the post response (whether is success or not)
     * The lambda is invoked when after the request is made.
     *
     * @param post The lambda to handle the post call response. It is suspended.
     */
    fun postCall(post: suspend () -> Unit) {
        this.post = post
    }
}