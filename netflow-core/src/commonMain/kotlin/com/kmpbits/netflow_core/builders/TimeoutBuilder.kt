package com.kmpbits.netflow_core.builders

import com.kmpbits.netflow_core.annotations.NetFlowMarker
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

@NetFlowMarker
class TimeoutBuilder internal constructor() {

    /**
     * Sets the default connect timeout for new connections. A value of 0 means no timeout.
     *
     * The connect timeout is applied when connecting a TCP socket to the target host. The default
     * value is 15 seconds.
     */
    var connectionTimeout: Duration = 15.seconds

    /**
     * Sets the default read timeout for new connections. A value of 0 means no timeout.
     *
     * The read timeout is applied to both the TCP socket and for individual read IO operations.
     *
     * The default value is 15 seconds.
     */
    var readTimeout: Duration = 15.seconds

    /**
     * Sets the default write timeout for new connections. A value of 0 means no timeout.
     *
     * The write timeout is applied for individual write IO operations. The default value is 15
     * seconds.
     */
    var writeTimeout: Duration = 15.seconds
}