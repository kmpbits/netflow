package com.kmpbits.netflow_core.builders

import com.kmpbits.netflow_core.annotations.NetFlowMarker

@NetFlowMarker
class ResponseBuilder<ApiType, DisplayType> @PublishedApi internal constructor() {

    @PublishedApi
    internal var offlineBuilder: OfflineBuilder<*>? = null

    @PublishedApi
    internal var transform: ((Any?) -> DisplayType)? = null

    @PublishedApi
    internal var apiTransform: ((ApiType) -> DisplayType)? = null

    @PublishedApi
    internal var post: (suspend () -> Unit)? = null

    @PublishedApi
    internal var onNetworkSuccess: (suspend (model: ApiType) -> Unit)? = null

    /**
     * Call this function to access the [OfflineBuilder] and handle local calls.
     *
     * The optional [transform] maps the local entity type [E] to [DisplayType].
     */
    @Suppress("UNCHECKED_CAST")
    fun <E> local(builder: OfflineBuilder<E>.() -> Unit, transform: ((E) -> DisplayType)? = null) {
        offlineBuilder = OfflineBuilder<E>().also(builder)
        transform?.let { transformFn ->
            this.transform = { transformFn(it as E) }
        }
    }

    /**
     * Call this function to map the api response type [ApiType] to [DisplayType].
     *
     * When [ApiType] and [DisplayType] are the same type this is not required.
     */
    fun apiTransform(transform: (ApiType) -> DisplayType) {
        this.apiTransform = transform
    }

    /**
     * Call this function to handle the success response and do anything with the data (ex: Inserting into database).
     * The lambda receives the raw [ApiType] before any display transform is applied.
     *
     * @param onSuccess The lambda with a success data response of type [ApiType]. It is suspended.
     */
    fun onNetworkSuccess(onSuccess: suspend (data: ApiType) -> Unit) {
        this.onNetworkSuccess = onSuccess
    }

    /**
     * Call this function to handle the post response (whether is success or not).
     * The lambda is invoked after the request is made.
     *
     * @param post The lambda to handle the post call response. It is suspended.
     */
    fun postCall(post: suspend () -> Unit) {
        this.post = post
    }
}
