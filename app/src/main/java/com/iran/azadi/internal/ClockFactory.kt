package com.iran.azadi.internal

import com.iran.azadi.internal.DefaultParam.CACHE_EXPIRATION_MS
import com.iran.azadi.internal.DefaultParam.MAX_NTP_RESPONSE_TIME_MS
import com.iran.azadi.internal.DefaultParam.MIN_WAIT_TIME_BETWEEN_SYNC_MS
import com.iran.azadi.internal.DefaultParam.NTP_HOSTS
import com.iran.azadi.internal.DefaultParam.TIMEOUT_MS
import com.iran.azadi.internal.internal.KronosClockImpl
import com.iran.azadi.internal.internal.ntp.DatagramFactoryImpl
import com.iran.azadi.internal.internal.ntp.DnsResolverImpl
import com.iran.azadi.internal.internal.ntp.SntpClient
import com.iran.azadi.internal.internal.ntp.SntpResponseCacheImpl
import com.iran.azadi.internal.internal.ntp.SntpServiceImpl

object ClockFactory {

    /**
     * Create a new instance of KronosClock which is synchronized with one of several
     * NTP server to ensure that the time is accurate. You should create only one instance
     * and keep it somewhere you can access throughout your application.
     *
     * @param syncResponseCache will be used to cache sync response so that time can be calculated after a successful clock sync
     * @param syncListener Allows you to log sync operation successes and errors.
     * @param ntpHosts Kronos synchronizes with this set of NTP servers. The default set of servers are ordered according to success rate from analytic
     * @param localClock local device clock that will be used as a fallback if NTP sync fails.
     * @param requestTimeoutMs Lengthen or shorten the timeout value. If the NTP server fails to respond within the given time, the next server will be contacted. If none of the server respond within the given time, the sync operation will be considered a failure.
     * @param minWaitTimeBetweenSyncMs Kronos attempts a synchronization at most once a minute. If you want to change the frequency, supply the desired time in milliseconds. Note that you should also supply a cacheExpirationMs value. For example, if you shorten the minWaitTimeBetweenSyncMs to 30 seconds, but leave the cacheExpirationMs to 1 minute, it will have no affect because the cache is still valid within the 1 minute window.
     * @param cacheExpirationMs Kronos will perform a background sync if the cache is stale. The cache is valid for 1 minute by default. It is simpliest to keep the cacheExpirationMs value the same as minWaitTimeBetweenSyncMs value.
     * @param maxNtpResponseTimeMs Kronos will consider a sync successful only if the NTP server response within the given time.
     *
     */
    @JvmStatic
    @JvmOverloads
    fun createKronosClock(
        localClock: Clock,
        syncResponseCache: SyncResponseCache,
        syncListener: SyncListener? = null,
        ntpHosts: List<String> = NTP_HOSTS,
        requestTimeoutMs: Long = TIMEOUT_MS,
        minWaitTimeBetweenSyncMs: Long = MIN_WAIT_TIME_BETWEEN_SYNC_MS,
        cacheExpirationMs: Long = CACHE_EXPIRATION_MS,
        maxNtpResponseTimeMs: Long = MAX_NTP_RESPONSE_TIME_MS
    ): KronosClock {

        if (localClock is KronosClock) {
            throw IllegalArgumentException("Local clock should implement Clock instead of KronosClock")
        }

        val sntpClient = SntpClient(localClock, DnsResolverImpl(), DatagramFactoryImpl())
        val cache = SntpResponseCacheImpl(syncResponseCache, localClock)
        val ntpService = SntpServiceImpl(
            sntpClient,
            localClock,
            cache,
            syncListener,
            ntpHosts,
            requestTimeoutMs,
            minWaitTimeBetweenSyncMs,
            cacheExpirationMs,
            maxNtpResponseTimeMs
        )
        return KronosClockImpl(ntpService, localClock)
    }
}
