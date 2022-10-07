package com.iran.azadi.internal

interface SyncResponseCache {
    var currentTime : Long
    var elapsedTime : Long
    var currentOffset: Long
    fun clear()
}