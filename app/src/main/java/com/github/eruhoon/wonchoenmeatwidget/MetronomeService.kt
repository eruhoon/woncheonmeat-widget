package com.github.eruhoon.wonchoenmeatwidget

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Handler
import android.os.HandlerThread
import android.os.IBinder
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.annotation.RequiresPermission
import androidx.core.app.NotificationCompat

class MetronomeService : Service() {
    private val toneGenerator = ToneGenerator(AudioManager.STREAM_MUSIC, 100)

    private var bpm = 120
    private var handler: Handler? = null
    private var beatRunnable: Runnable? = null
    private var handlerThread: HandlerThread? = null // Handler를 위한 별도 스레드

    companion object {
        const val ACTION_START = "com.github.eruhoon.wonchoenmeatwidget.ACTION_START"
        const val ACTION_STOP = "com.github.eruhoon.wonchoenmeatwidget.ACTION_STOP"
        const val EXTRA_BPM = "BPM"
    }

    override fun onCreate() {
        super.onCreate()
        // HandlerThread를 사용하여 백그라운드 스레드에서 Handler 실행
        handlerThread = HandlerThread("MetronomeHandlerThread").apply {
            start()
            handler = Handler(looper) // 생성된 스레드의 Looper 사용
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                bpm = intent.getIntExtra(EXTRA_BPM, 120)
                if (beatRunnable == null) { // 중복 실행 방지
                    beatRunnable = object : Runnable {
                        override fun run() {
                            playBeep()
                            handler?.postDelayed(this, (60000 / bpm).toLong())
                        }
                    }
                    handler?.post(beatRunnable!!)
                    startForeground(
                        1,
                        createNotification("Metronome Running at $bpm BPM")
                    ) // 알림 내용에 BPM 표시
                }
            }

            ACTION_STOP -> {
                stopPlayback()
            }

            else -> { // 인텐트 액션이 없거나 모르는 경우 (예: 위젯에서 직접 서비스 시작 시)
                bpm = intent?.getIntExtra(EXTRA_BPM, 120) ?: 120 // BPM 값 가져오기
                if (beatRunnable == null) {
                    beatRunnable = object : Runnable {
                        override fun run() {
                            playBeep()
                            handler?.postDelayed(this, (60000 / bpm).toLong())
                        }
                    }
                    handler?.post(beatRunnable!!)
                    startForeground(1, createNotification("Metronome Running at $bpm BPM"))
                }
            }
        }
        return START_STICKY
    }

    private fun stopPlayback() {
        handler?.removeCallbacks(beatRunnable!!) // 반복 작업 제거
        beatRunnable = null // Runnable 참조 제거
        stopForeground(STOP_FOREGROUND_REMOVE) // 알림 제거하며 포그라운드 서비스 종료 (API 24+)
        // 또는 stopForeground(true) 사용 가능
        stopSelf() // 서비스 종료
    }

    // createNotification 함수 수정 (내용을 파라미터로 받도록)
    private fun createNotification(contentText: String): Notification {
        val channelId = "metronome_service"
        val channel =
            NotificationChannel(channelId, "Metronome", NotificationManager.IMPORTANCE_LOW)
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        return NotificationCompat.Builder(this, channelId)
            .setContentTitle("Metronome Active")
            .setContentText(contentText)
            .setSmallIcon(android.R.drawable.ic_media_play)
            .build()
    }

    @RequiresPermission(Manifest.permission.VIBRATE)
    private fun playBeep() {
        toneGenerator.startTone(ToneGenerator.TONE_PROP_BEEP, 70)
//        val vibrator = getSystemService(VIBRATOR_SERVICE) as Vibrator
//        vibrator.vibrate(VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE))
    }

    override fun onDestroy() {
        super.onDestroy()
        // HandlerThread 정리
        handlerThread?.quitSafely()
        handlerThread = null
        handler = null
        beatRunnable = null // 참조 확실히 제거
    }

    override fun onBind(intent: Intent?): IBinder? = null
}