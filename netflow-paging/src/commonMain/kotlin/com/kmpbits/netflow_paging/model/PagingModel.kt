package com.kmpbits.netflow_paging.model

abstract class PagingModel {
    var page: Int = 1
    var lastUpdatedTimestamp: Long? = null
}