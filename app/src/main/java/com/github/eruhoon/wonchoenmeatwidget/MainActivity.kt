package com.github.eruhoon.wonchoenmeatwidget

import android.Manifest
import android.app.Activity
import android.content.Context
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.util.Log
import android.widget.Button
import android.widget.EditText
import androidx.annotation.RequiresPermission

class MainActivity : Activity() {
    private var toneGenerator: ToneGenerator? = null

    private var bpm = 120
    private var durationMin = 0
    private var isRunning = false
    private var handler: Handler? = null
    private var beatRunnable: Runnable? = null

    private var endTime: Long = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val bpmInput = findViewById<EditText>(R.id.bpmInput)
        val durationInput = findViewById<EditText>(R.id.durationInput)
        val startButton = findViewById<Button>(R.id.startButton)

        handler = Handler(Looper.getMainLooper())
        beatRunnable = object : Runnable {
            @RequiresPermission(Manifest.permission.VIBRATE)
            override fun run() {
                if (System.currentTimeMillis() >= endTime) {
                    stopMetronome()
                    return
                }
                playBeep(applicationContext)
                handler?.postDelayed(this, (60000 / bpm).toLong())
            }
        }

        startButton.setOnClickListener {
            bpm = bpmInput.text.toString().toIntOrNull() ?: 120
            durationMin = durationInput.text.toString().toIntOrNull() ?: 0
            if (!isRunning) {
                endTime = if (durationMin > 0) System.currentTimeMillis() + durationMin * 60_000 else Long.MAX_VALUE
                handler?.post(beatRunnable!!)
                isRunning = true
                startButton.text = "Stop"
            } else {
                stopMetronome()
                startButton.text = "Start"
            }
        }
    }

    private fun stopMetronome() {
        handler?.removeCallbacks(beatRunnable!!)
        isRunning = false
        findViewById<Button>(R.id.startButton).text = "Start"
    }

    @RequiresPermission(Manifest.permission.VIBRATE)
    private fun playBeep(context: Context) {
        try {
            if (toneGenerator == null) {
                toneGenerator = ToneGenerator(AudioManager.STREAM_MUSIC, 100)
            }
            toneGenerator?.startTone(ToneGenerator.TONE_PROP_BEEP, 100)
        } catch (e: RuntimeException) {
            Log.e("Metronome", "ToneGenerator init failed", e)
            toneGenerator = null
        }

        val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        vibrator.vibrate(VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE))
    }

    override fun onDestroy() {
        super.onDestroy()
        toneGenerator?.release()
        toneGenerator = null
    }
}