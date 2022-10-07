package com.iran.azadi

import android.content.Context
import com.iran.azadi.internal.AndroidSystemClock
import com.iran.azadi.internal.SharedPreferenceSyncResponseCache
import com.iran.azadi.internal.Clock
import com.iran.azadi.internal.DefaultParam.CACHE_EXPIRATION_MS
import com.iran.azadi.internal.DefaultParam.MAX_NTP_RESPONSE_TIME_MS
import com.iran.azadi.internal.DefaultParam.MIN_WAIT_TIME_BETWEEN_SYNC_MS
import com.iran.azadi.internal.DefaultParam.NTP_HOSTS
import com.iran.azadi.internal.DefaultParam.TIMEOUT_MS
import com.iran.azadi.internal.ClockFactory
import com.iran.azadi.internal.KronosClock
import com.iran.azadi.internal.SyncListener

object AndroidClockFactory {

    /**
     * Create a device clock that uses the OS/device specific API to retrieve time
     */
    @JvmStatic
    fun createDeviceClock(): Clock = AndroidSystemClock()

    @JvmStatic
    @JvmOverloads
    fun createKronosClock(
        context: Context,
        syncListener: SyncListener? = null,
        ntpHosts: List<String> = NTP_HOSTS,
        requestTimeoutMs: Long = TIMEOUT_MS,
        minWaitTimeBetweenSyncMs: Long = MIN_WAIT_TIME_BETWEEN_SYNC_MS,
        cacheExpirationMs: Long = CACHE_EXPIRATION_MS,
        maxNtpResponseTimeMs: Long = MAX_NTP_RESPONSE_TIME_MS
    ): KronosClock {

        val deviceClock = createDeviceClock()
        val cache = SharedPreferenceSyncResponseCache(
            context.getSharedPreferences(
                SharedPreferenceSyncResponseCache.SHARED_PREFERENCES_NAME, Context.MODE_PRIVATE
            )
        )

        return ClockFactory.createKronosClock(
            deviceClock,
            cache,
            syncListener,
            ntpHosts,
            requestTimeoutMs,
            minWaitTimeBetweenSyncMs,
            cacheExpirationMs,
            maxNtpResponseTimeMs
        )
    }
}