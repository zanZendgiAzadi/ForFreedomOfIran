package com.iran.azadi.internal.internal.ntp

import com.iran.azadi.internal.Clock
import com.iran.azadi.internal.SyncResponseCache
import com.iran.azadi.internal.internal.Constants.TIME_UNAVAILABLE

internal interface SntpResponseCache {

    fun get(): SntpClient.Response?

    fun update(response: SntpClient.Response)

    fun clear()
}

internal class SntpResponseCacheImpl(
    private val syncResponseCache: SyncResponseCache,
    private val deviceClock: Clock,
) : SntpResponseCache {

    override fun get(): SntpClient.Response? {
        val currentTime = syncResponseCache.currentTime
        val elapsedTime = syncResponseCache.elapsedTime
        val currentOffset = syncResponseCache.currentOffset
        return when (elapsedTime) {
            TIME_UNAVAILABLE -> null
            else -> SntpClient.Response(currentTime, elapsedTime, currentOffset, deviceClock)
        }
    }

    override fun update(response: SntpClient.Response) {
        synchronized(this) {
            syncResponseCache.currentTime = response.deviceCurrentTimestampMs
            syncResponseCache.elapsedTime = response.deviceElapsedTimestampMs
            syncResponseCache.currentOffset = response.offsetMs
        }
    }

    override fun clear() {
        synchronized(this) {
            syncResponseCache.clear()
        }
    }
}
