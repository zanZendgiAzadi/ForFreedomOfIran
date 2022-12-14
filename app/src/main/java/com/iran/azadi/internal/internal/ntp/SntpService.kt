package com.iran.azadi.internal.internal.ntp

import com.iran.azadi.internal.Clock
import com.iran.azadi.internal.DefaultParam.CACHE_EXPIRATION_MS
import com.iran.azadi.internal.DefaultParam.MAX_NTP_RESPONSE_TIME_MS
import com.iran.azadi.internal.DefaultParam.MIN_WAIT_TIME_BETWEEN_SYNC_MS
import com.iran.azadi.internal.DefaultParam.TIMEOUT_MS
import com.iran.azadi.internal.KronosTime
import com.iran.azadi.internal.SyncListener
import com.iran.azadi.internal.internal.Constants
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.atomic.AtomicReference

internal interface SntpService {

    /**
     * Return the current time in millisecond.
     *
     * If NTP server cannot be reached or if it hasn't yet synced up to the NTP server,
     * it will return [Constants.TIME_UNAVAILABLE] instead.
     *
     * Note calling this method will trigger [syncInBackground] being called when necessary.
     * You should still call [sync]/[syncInBackground] once to ensure you have the correct
     * as soon as possible. By default [sync] will be triggered at most once a minute.
     *
     * @return the current time in milliseconds.
     */
    fun currentTimeMs(): Long {
        return this.currentTime()?.posixTimeMs ?: Constants.TIME_UNAVAILABLE
    }

    /**
     * Calls [sync] in a background thread. This method returns immediately.
     */
    fun syncInBackground()

    /**
     * synchronize time with one of several NTP server. This method returns true on the first
     * successful response. This method returns false if we failed to get data from any
     * NTP server.
     *
     * @return true if we successfully synchronize with an NTP server.
     */
    fun sync(): Boolean

    /**
     * You should call [shutdown] when you no longer need this service. This will shutdown
     * the thread that performs syncing in the background. Any subsequent call to [currentTime]
     * will throw [IllegalStateException].
     */
    fun shutdown()

    /**
     * Same as currentTimeMs(), but returned object includes time since last ntp sync.
     *
     * @return the current time, or null if NTP cannot be reached / hasn't synced
     */
    fun currentTime(): KronosTime?
}

internal class SntpServiceImpl @JvmOverloads constructor(private val sntpClient: SntpClient,
                                                         private val deviceClock: Clock,
                                                         private val responseCache: SntpResponseCache,
                                                         private val ntpSyncListener: SyncListener?,
                                                         private val ntpHosts: List<String>,
                                                         private val requestTimeoutMs: Long = TIMEOUT_MS,
                                                         private val minWaitTimeBetweenSyncMs: Long = MIN_WAIT_TIME_BETWEEN_SYNC_MS,
                                                         private val cacheExpirationMs: Long = CACHE_EXPIRATION_MS,
                                                         private val maxNtpResponseTimeMs: Long = MAX_NTP_RESPONSE_TIME_MS) :
    SntpService {

    private val state = AtomicReference(State.IDLE)
    private val cachedSyncTime = AtomicLong(0)
    private val executor = Executors.newSingleThreadExecutor { Thread(it, "kronos-android") }

    private val response: SntpClient.Response?
        get() {
            val response = responseCache.get()
            val isCachedFromPreviousBoot = state.get() == State.IDLE && response != null && !response.isFromSameBoot
            return if (isCachedFromPreviousBoot) {
                responseCache.clear()
                null
            } else {
                response
            }
        }

    private val cacheSyncAge: Long
        get() = deviceClock.getElapsedTimeMs() - cachedSyncTime.get()

    private enum class State {
        IDLE,
        SYNCING,
        STOPPED
    }

    override fun currentTime(): KronosTime? {
        ensureServiceIsRunning()

        val response = response
        if (response == null) {
            if (cacheSyncAge >= minWaitTimeBetweenSyncMs) {
                syncInBackground()
            }
            return null // No time fetched
        }
        val responseAge = response.responseAge
        if (responseAge >= cacheExpirationMs && cacheSyncAge >= minWaitTimeBetweenSyncMs) {
            syncInBackground()
        }

        return KronosTime(posixTimeMs = response.currentTimeMs, timeSinceLastNtpSyncMs = responseAge)
    }

    override fun syncInBackground() {
        ensureServiceIsRunning()

        if (state.get() != State.SYNCING) {
            executor.submit { sync() }
        }
    }

    override fun shutdown() {
        ensureServiceIsRunning()
        state.set(State.STOPPED)
        executor.shutdown()
    }

    override fun sync(): Boolean {
        ensureServiceIsRunning()

        for (host in ntpHosts) {
            if (sync(host)) {
                return true
            }
        }
        return false
    }

    private fun ensureServiceIsRunning() {
        if (state.get() == State.STOPPED) {
            throw IllegalStateException("Service already shutdown")
        }
    }

    private fun sync(host: String): Boolean {
        if (state.getAndSet(State.SYNCING) != State.SYNCING) {
            val t1 = deviceClock.getElapsedTimeMs()
            ntpSyncListener?.onStartSync(host)
            try {
                val response = sntpClient.requestTime(host, requestTimeoutMs)
                if (response.currentTimeMs < 0) {
                    throw NTPSyncException("Invalid time ${response.currentTimeMs} received from $host")
                }
                val responseTime = deviceClock.getElapsedTimeMs() - t1
                if (responseTime > maxNtpResponseTimeMs) {
                    throw NTPSyncException("Ignoring response from $host because the network latency ($responseTime ms) is longer than the required value ($maxNtpResponseTimeMs ms")
                }
                responseCache.update(response)
                val cachedOffset = response.offsetMs
                ntpSyncListener?.onSuccess(cachedOffset, responseTime)
                return true

            } catch (e: Throwable) {
                ntpSyncListener?.onError(host, e)
            } finally {
                state.set(State.IDLE)
                cachedSyncTime.set(deviceClock.getElapsedTimeMs())
            }
        }
        return false
    }
}

