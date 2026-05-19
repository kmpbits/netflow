package com.kmpbits.netflow_paging.model

abstract class PagingModel {
    internal var page: Int = 1
    internal var lastUpdatedTimestamp: Long? = null
}