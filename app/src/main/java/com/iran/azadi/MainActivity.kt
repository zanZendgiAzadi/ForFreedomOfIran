package com.iran.azadi

import android.net.Uri
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.Player
import com.iran.azadi.internal.KronosClock
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : AppCompatActivity() {

    lateinit var kronosClock: KronosClock
    private val player by lazy {
        ExoPlayer.Builder(this)
            .build()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        kronosClock = AndroidClockFactory.createKronosClock(applicationContext)
        kronosClock.syncInBackground()

        val firstVideoUri = Uri.parse("asset:///azadi.mp3")
        val firstItem = MediaItem.fromUri(firstVideoUri)
        player.addMediaItem(firstItem)
        player.prepare()
        makeMusicSync()
        player.addListener(object : Player.Listener {
            override fun onPlaybackStateChanged(state: Int) {
                if (state == Player.STATE_ENDED) {
                    makeMusicSync()
                }
            }
        })
    }

    private fun makeMusicSync() {
        GlobalScope.launch {
            delay(1000)
            if (isServerTimeSyncedBefore()) {
                kronosClock.syncInBackground()
                play()
            } else if (kronosClock.sync()) {
                play()
            } else {
                makeMusicSync()
            }
        }
    }

    private suspend fun play() {
        withContext(Dispatchers.Main) {
            val position = kronosClock.getCurrentTimeMs() % player.duration
            player.seekTo(position)
            player.play()
        }
    }

    private fun isServerTimeSyncedBefore() =
        kronosClock.getCurrentTime().timeSinceLastNtpSyncMs != null
}