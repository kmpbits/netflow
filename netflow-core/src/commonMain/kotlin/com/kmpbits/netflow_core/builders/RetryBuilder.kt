package com.kmpbits.netflow_core.builders

import com.kmpbits.netflow_core.annotations.NetFlowMarker
import com.kmpbits.netflow_core.enums.RetryTimes
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

@NetFlowMarker
class RetryBuilder internal constructor() {

    /**
     * Total number of attempts including the initial request.
     * For example, `times = RetryTimes.THREE` means 1 initial try + 2 retries.
     *
     * Allowed values are from ONE (1 attempt) up to FIVE (5 attempts).
     * More than five retries is generally discouraged as it may overwhelm the server.
     */
    var times: RetryTimes = RetryTimes.ONE

    /**
     * Delay between retries. Default is 500 milliseconds.
     * You can set it like `1.seconds` or `500.milliseconds`.
     */
    var delay: Duration = 500.milliseconds

    /**
     * Optional lambda that determines whether a retry should happen for a given exception.
     * Return `true` to retry, or `false` to stop retrying on that error.
     * If not provided, all exceptions will trigger a retry (up to [times]).
     */
    var retryOn: (suspend (Throwable) -> Boolean)? = null
}