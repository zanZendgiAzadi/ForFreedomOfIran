package com.iran.azadi.internal.internal

import com.iran.azadi.internal.Clock
import com.iran.azadi.internal.KronosClock
import com.iran.azadi.internal.KronosTime
import com.iran.azadi.internal.internal.ntp.SntpService

internal class KronosClockImpl(private val ntpService: SntpService, private val fallbackClock: Clock) :
    KronosClock {

    override fun sync() = ntpService.sync()

    override fun syncInBackground() = ntpService.syncInBackground()

    override fun shutdown() = ntpService.shutdown()

    override fun getElapsedTimeMs(): Long = fallbackClock.getElapsedTimeMs()

    override fun getCurrentTime(): KronosTime {
        val currentTime = ntpService.currentTime()
        return currentTime ?: KronosTime(posixTimeMs = fallbackClock.getCurrentTimeMs(), timeSinceLastNtpSyncMs = null)
    }

    override fun getCurrentNtpTimeMs() : Long? = ntpService.currentTime()?.posixTimeMs
}