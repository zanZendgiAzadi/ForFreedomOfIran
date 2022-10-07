package com.iran.azadi.internal

import android.os.SystemClock

internal class AndroidSystemClock : Clock {
    override fun getCurrentTimeMs(): Long = System.currentTimeMillis()
    override fun getElapsedTimeMs(): Long = SystemClock.elapsedRealtime()
}