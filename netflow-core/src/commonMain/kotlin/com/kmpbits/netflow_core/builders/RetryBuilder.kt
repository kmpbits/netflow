package com.kmpbits.netflow_core.builders

import com.kmpbits.netflow_core.annotations.NetFlowMarker
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

@NetFlowMarker
class RetryBuilder internal constructor() {

    /**
     * Total number of attempts including the initial request.
     * For example, `times = 3` means 1 initial try + 2 retries.
     * Must be >= 1.
     */
    var times = 1

    /**
     * Delay between retries. Default is zero.
     * You can set it like `1.seconds` or `500.milliseconds`.
     */
    var delay: Duration = 0.seconds

    /**
     * Optional lambda that determines whether a retry should happen for a given exception.
     * Return `true` to retry, or `false` to stop retrying on that error.
     * If not provided, all exceptions will trigger a retry (up to [times]).
     */
    var retryOn: (suspend (Throwable) -> Boolean)? = null
}