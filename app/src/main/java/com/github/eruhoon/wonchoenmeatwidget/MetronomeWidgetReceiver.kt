package com.github.eruhoon.wonchoenmeatwidget

import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.glance.GlanceId
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.state.updateAppWidgetState
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch

class MetronomeWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = MetronomeWidget()

    private val coroutineScope = MainScope()
    private val TAG = "MetronomeWidgetReceiver"

    companion object {
        const val ACTION_INCREMENT_BPM =
            "com.github.eruhoon.wonchoenmeatwidget.ACTION_INCREMENT_BPM"
        const val ACTION_DECREMENT_BPM =
            "com.github.eruhoon.wonchoenmeatwidget.ACTION_DECREMENT_BPM"
        const val ACTION_TOGGLE_PLAYBACK =
            "com.github.eruhoon.wonchoenmeatwidget.ACTION_TOGGLE_PLAYBACK"
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)

        val action = intent.action
        if (action != ACTION_INCREMENT_BPM &&
            action != ACTION_DECREMENT_BPM &&
            action != ACTION_TOGGLE_PLAYBACK
        ) {
            // Log.d(TAG, "Ignoring action: $action")
            return
        }

        val appWidgetId = intent.getIntExtra(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID
        )

        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            Log.e(TAG, "Invalid AppWidgetId. Action aborted for action: $action")
            return
        }

        coroutineScope.launch {
            try {
                val glanceId = GlanceAppWidgetManager(context).getGlanceIdBy(appWidgetId)
                when (action) {
                    ACTION_INCREMENT_BPM, ACTION_DECREMENT_BPM -> {
                        processBpmAction(context, glanceId, action, appWidgetId)
                    }

                    ACTION_TOGGLE_PLAYBACK -> {
                        processTogglePlaybackAction(context, glanceId, appWidgetId)
                    }
                }
            } catch (e: Exception) {
                Log.e(
                    TAG,
                    "Error processing action for appWidgetId $appWidgetId. Action: $action",
                    e
                )
            }
        }
    }

    private suspend fun processBpmAction(
        context: Context,
        glanceId: GlanceId,
        action: String?,
        appWidgetId: Int
    ) {
        Log.d(
            TAG,
            "Processing BPM action $action for glanceId: $glanceId (appWidgetId: $appWidgetId)"
        )
        updateAppWidgetState(context, glanceId) { prefs ->
            val currentBpm = prefs[MetronomeWidgetState.BPM_KEY] ?: MetronomeWidgetState.DEFAULT_BPM
            val newBpm = when (action) {
                ACTION_INCREMENT_BPM -> (currentBpm + 10).coerceAtMost(180)
                ACTION_DECREMENT_BPM -> (currentBpm - 10).coerceAtLeast(120)
                else -> currentBpm // Should not happen
            }
            prefs[MetronomeWidgetState.BPM_KEY] = newBpm
        }
        glanceAppWidget.update(context, glanceId)
        Log.d(TAG, "Updated BPM for $glanceId")
    }

    private suspend fun processTogglePlaybackAction(
        context: Context,
        glanceId: GlanceId,
        appWidgetId: Int
    ) {
        Log.d(
            TAG,
            "Processing Toggle Playback action for glanceId: $glanceId (appWidgetId: $appWidgetId)"
        )
        var isPlaying = false // 현재 재생 상태를 읽어오기 위한 변수
        var currentBpm = MetronomeWidgetState.DEFAULT_BPM

        // 먼저 현재 상태를 읽어온다.
        updateAppWidgetState(context, glanceId) { prefs ->
            isPlaying = prefs[MetronomeWidgetState.IS_PLAYING_KEY]
                ?: MetronomeWidgetState.DEFAULT_IS_PLAYING
            currentBpm = prefs[MetronomeWidgetState.BPM_KEY] ?: MetronomeWidgetState.DEFAULT_BPM
            // 상태를 토글한다.
            prefs[MetronomeWidgetState.IS_PLAYING_KEY] = !isPlaying
        }

        // 상태를 토글한 후, 서비스 제어
        val serviceIntent = Intent(context, MetronomeService::class.java)
        if (!isPlaying) {
            serviceIntent.action = MetronomeService.ACTION_START
            serviceIntent.putExtra(MetronomeService.EXTRA_BPM, currentBpm)
            context.startService(serviceIntent)
            Log.d(TAG, "Started MetronomeService for $glanceId at $currentBpm BPM")
        } else {
            serviceIntent.action = MetronomeService.ACTION_STOP
            context.startService(serviceIntent)
            Log.d(TAG, "Stopped MetronomeService for $glanceId")
        }

        glanceAppWidget.update(context, glanceId) // 위젯 UI 업데이트 (버튼 텍스트 등 변경)
    }
}