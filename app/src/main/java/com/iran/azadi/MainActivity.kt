package com.iran.azadi

import android.net.Uri
import android.os.Bundle
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.Player
import com.iran.azadi.internal.KronosClock
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var kronosClock: KronosClock
    private val player by lazy {
        ExoPlayer.Builder(this)
            .build()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        kronosClock = AndroidClockFactory.createKronosClock(applicationContext)
        val mediaUri = Uri.parse("asset:///azadi.mp3")
        val mediaItem = MediaItem.fromUri(mediaUri)
        player.addMediaItem(mediaItem)
        player.prepare()

        if (isServerTimeSyncedBefore().not()) {
            if (kronosClock.sync()) play()
        } else {
            makeMusicSync()
        }
        player.addListener(object : Player.Listener {
            override fun onPlaybackStateChanged(state: Int) {
                if (state == Player.STATE_ENDED) {
                    makeMusicSync()
                }
            }
        })
    }

    override fun onStart() {
        super.onStart()
        if (player.isPlaying.not()) {
            makeMusicSync()
        }
    }

    override fun onBackPressed() {
        super.onBackPressed()
        player.playWhenReady = false
    }

    override fun onDestroy() {
        super.onDestroy()
        player.release()
    }

    private fun makeMusicSync() {
        GlobalScope.launch(Dispatchers.Main) {
            if (isServerTimeSyncedBefore()) {
                play()
                return@launch
            } else if (kronosClock.sync()) {
                play()
                return@launch
            } else {
                makeMusicSync()
                Toast.makeText(
                    this@MainActivity,
                    getString(R.string.enable_internet),
                    Toast.LENGTH_SHORT
                ).show()
                delay(4000)
            }
        }
    }

    private fun play() {
        val position = kronosClock.getCurrentTimeMs() % player.duration
        player.seekTo(position)
        player.play()
        findViewById<TextView>(R.id.txtLoading).isVisible = false
        findViewById<ProgressBar>(R.id.progressBar).isVisible = false
    }

    private fun isServerTimeSyncedBefore() =
        kronosClock.getCurrentTime().timeSinceLastNtpSyncMs != null
}