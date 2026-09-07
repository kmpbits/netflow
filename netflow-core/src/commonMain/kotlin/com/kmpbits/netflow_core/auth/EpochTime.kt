package com.kmpbits.netflow_core.auth

/** Current wall-clock time in seconds since the Unix epoch. Platform-provided, no dependency. */
internal expect fun currentEpochSeconds(): Long
