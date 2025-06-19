package com.kmpbits.netflow_core.exceptions

class NetFlowException(override val message: String?) : IllegalStateException(message)