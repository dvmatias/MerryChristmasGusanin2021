package com.example.merrychristmasgusanin2021

import androidx.appcompat.app.AppCompatActivity
import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.view.MotionEvent
import android.view.View
import com.example.merrychristmasgusanin2021.databinding.ActivityFullscreenBinding
import android.widget.Toast

import android.bluetooth.BluetoothA2dp
import android.util.Log
import android.content.res.AssetFileDescriptor

import android.media.MediaPlayer
import android.media.AudioManager

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothProfile

import android.bluetooth.BluetoothProfile.ServiceListener
import android.content.IntentFilter
import android.media.AudioAttributes
import android.media.MediaPlayer.OnPreparedListener
import java.lang.Exception


/**
 * An example full-screen activity that shows and hides the system UI (i.e.
 * status bar and navigation/system bar) with user interaction.
 */
class FullscreenActivity : AppCompatActivity() {

    private lateinit var binding: ActivityFullscreenBinding
    private val hideHandler = Handler()

    lateinit var btAdapter: BluetoothAdapter
    lateinit var a2dpService: BluetoothA2dp
    lateinit var audioManager: AudioManager
    private var mediaPlayer: MediaPlayer? = null
    private var isA2dpReady = false
        set(value) {
            field = value
            Toast.makeText(this, "A2DP ready ? " + if (value) "true" else "false", Toast.LENGTH_SHORT)
                .show()
        }
    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(p0: Context?, p1: Intent?) {
//            val action = intent.action
//            Log.d(TAG, "receive intent for action : $action")
//            if (action == BluetoothA2dp.ACTION_CONNECTION_STATE_CHANGED) {
//                val state =
//                    intent.getIntExtra(BluetoothA2dp.EXTRA_STATE, BluetoothA2dp.STATE_DISCONNECTED)
//                if (state == BluetoothA2dp.STATE_CONNECTED) {
//                    isA2dpReady = true
//                    playMusic()
//                } else if (state == BluetoothA2dp.STATE_DISCONNECTED) {
//                    isA2dpReady = false
//                }
//            } else if (action == BluetoothA2dp.ACTION_PLAYING_STATE_CHANGED) {
//                val state =
//                    intent.getIntExtra(BluetoothA2dp.EXTRA_STATE, BluetoothA2dp.STATE_NOT_PLAYING)
//                if (state == BluetoothA2dp.STATE_PLAYING) {
//                    Log.d(TAG, "A2DP start playing")
//                    Toast.makeText(this@FullscreenActivity, "A2dp is playing", Toast.LENGTH_SHORT)
//                        .show()
//                } else {
//                    Log.d(TAG, "A2DP stop playing")
//                    Toast.makeText(this@FullscreenActivity, "A2dp is stopped", Toast.LENGTH_SHORT)
//                        .show()
//                }
//            }
        }
    }
    private val mA2dpListener: ServiceListener = object : ServiceListener {
        override fun onServiceConnected(profile: Int, a2dp: BluetoothProfile) {
//            Log.d(TAG, "a2dp service connected. profile = $profile")
//            if (profile == BluetoothProfile.A2DP) {
//                a2dpService = a2dp as BluetoothA2dp
//                if (audioManager.isBluetoothA2dpOn) {
//                    isA2dpReady = true
//                    playMusic()
//                } else {
//                    Log.d(TAG, "bluetooth a2dp is not on while service connected")
//                }
//            }
        }

        override fun onServiceDisconnected(profile: Int) {
            isA2dpReady = false
        }
    }

    @SuppressLint("InlinedApi")
    private val hidePart2Runnable = Runnable {
        // Delayed removal of status and navigation bar
        if (Build.VERSION.SDK_INT >= 30) {
            // fullscreenContent.windowInsetsController?.hide(WindowInsets.Type.statusBars() or WindowInsets.Type.navigationBars())
        } else {
            // Note that some of these constants are new as of API 16 (Jelly Bean)
            // and API 19 (KitKat). It is safe to use them, as they are inlined
            // at compile-time and do nothing on earlier devices.
           // fullscreenContent.systemUiVisibility =
                View.SYSTEM_UI_FLAG_LOW_PROFILE or
                        View.SYSTEM_UI_FLAG_FULLSCREEN or
                        View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
                        View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or
                        View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or
                        View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
        }
    }
    private val showPart2Runnable = Runnable {
        // Delayed display of UI elements
        supportActionBar?.show()
       // fullscreenContentControls.visibility = View.VISIBLE
    }
    private var isFullscreen: Boolean = false

    private val hideRunnable = Runnable { hide() }

    /**
     * Touch listener to use for in-layout UI controls to delay hiding the
     * system UI. This is to prevent the jarring behavior of controls going away
     * while interacting with activity UI.
     */
    private val delayHideTouchListener = View.OnTouchListener { view, motionEvent ->
        when (motionEvent.action) {
            MotionEvent.ACTION_DOWN -> if (AUTO_HIDE) {
                delayedHide(AUTO_HIDE_DELAY_MILLIS)
            }
            MotionEvent.ACTION_UP -> view.performClick()
            else -> {
            }
        }
        false
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityFullscreenBinding.inflate(layoutInflater)
        setContentView(binding.root)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        isFullscreen = true

        audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        registerReceiver(receiver, IntentFilter(BluetoothA2dp.ACTION_CONNECTION_STATE_CHANGED));
        registerReceiver(receiver, IntentFilter(BluetoothA2dp.ACTION_PLAYING_STATE_CHANGED));

        btAdapter = BluetoothAdapter.getDefaultAdapter().also {
            it.getProfileProxy(this, mA2dpListener , BluetoothProfile.A2DP)
        }

        setAudioOutput(OutputType.SPEAKERS)
        binding.btButton.setOnClickListener {
            setAudioOutput(OutputType.BLUETOOTH_DEVICE)
        }
        binding.spButton.setOnClickListener {
            setAudioOutput(OutputType.SPEAKERS)
        }

        playMusic()
    }

    private fun setAudioOutput(outputType: OutputType) {
        with(audioManager) {
            when(outputType) {
                OutputType.BLUETOOTH_DEVICE -> {
                    startBluetoothSco()
                    setBluetoothScoOn(true)
                    setSpeakerphoneOn(false)
                }
                OutputType.SPEAKERS -> {
                    setMode(AudioManager.MODE_IN_CALL);
                    stopBluetoothSco()
                    setBluetoothScoOn(false)
                    setSpeakerphoneOn(true)
                }
            }
        }
    }

    override fun onPostCreate(savedInstanceState: Bundle?) {
        super.onPostCreate(savedInstanceState)

        // Trigger the initial hide() shortly after the activity has been
        // created, to briefly hint to the user that UI controls
        // are available.
        delayedHide(100)
    }

    override fun onDestroy() {
        btAdapter.closeProfileProxy(BluetoothProfile.A2DP, a2dpService)
        releaseMediaPlayer()
        unregisterReceiver(receiver)
        super.onDestroy()
    }

    override fun onPause() {
        super.onPause()
        releaseMediaPlayer()
    }

    private fun playMusic() {
        val assetManager = this.assets
        val fd: AssetFileDescriptor
        mediaPlayer = MediaPlayer()
        mediaPlayer?.setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                .setLegacyStreamType(AudioManager.STREAM_VOICE_CALL)
                .build()
        ).also {
            try {
                fd = assetManager.openFd("music.mp3")
                Log.d(TAG, "fd = $fd")
                mediaPlayer?.apply {
                    setOnPreparedListener { mp -> mp.start() }
                    setDataSource(fd.fileDescriptor, fd.startOffset, fd.length)
                    prepareAsync()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun releaseMediaPlayer() {
        mediaPlayer?.stop()
        mediaPlayer?.release()
        mediaPlayer = null
    }

    private fun toggle() {
        if (isFullscreen) {
            hide()
        } else {
            show()
        }
    }

    private fun hide() {
        // Hide UI first
        supportActionBar?.hide()
       // fullscreenContentControls.visibility = View.GONE
        isFullscreen = false

        // Schedule a runnable to remove the status and navigation bar after a delay
        hideHandler.removeCallbacks(showPart2Runnable)
        hideHandler.postDelayed(hidePart2Runnable, UI_ANIMATION_DELAY.toLong())
    }

    private fun show() {
        // Show the system bar
        if (Build.VERSION.SDK_INT >= 30) {
         //   fullscreenContent.windowInsetsController?.show(WindowInsets.Type.statusBars() or WindowInsets.Type.navigationBars())
        } else {
          //  fullscreenContent.systemUiVisibility =
                View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
                        View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
        }
        isFullscreen = true

        // Schedule a runnable to display UI elements after a delay
        hideHandler.removeCallbacks(hidePart2Runnable)
        hideHandler.postDelayed(showPart2Runnable, UI_ANIMATION_DELAY.toLong())
    }

    /**
     * Schedules a call to hide() in [delayMillis], canceling any
     * previously scheduled calls.
     */
    private fun delayedHide(delayMillis: Int) {
        hideHandler.removeCallbacks(hideRunnable)
        hideHandler.postDelayed(hideRunnable, delayMillis.toLong())
    }

    companion object {
        const val TAG = "FullscreenActivity"

        /**
         * Whether or not the system UI should be auto-hidden after
         * [AUTO_HIDE_DELAY_MILLIS] milliseconds.
         */
        private const val AUTO_HIDE = true

        /**
         * If [AUTO_HIDE] is set, the number of milliseconds to wait after
         * user interaction before hiding the system UI.
         */
        private const val AUTO_HIDE_DELAY_MILLIS = 3000

        /**
         * Some older devices needs a small delay between UI widget updates
         * and a change of the status and navigation bar.
         */
        private const val UI_ANIMATION_DELAY = 300
    }
}