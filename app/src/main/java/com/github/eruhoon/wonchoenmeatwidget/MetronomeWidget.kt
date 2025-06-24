package com.github.eruhoon.wonchoenmeatwidget

import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.Button
import androidx.glance.ButtonDefaults
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.LocalContext
import androidx.glance.LocalGlanceId
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.action.actionSendBroadcast
import androidx.glance.appwidget.provideContent
import androidx.glance.currentState
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.state.PreferencesGlanceStateDefinition
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import kotlinx.coroutines.launch

class MetronomeWidget : GlanceAppWidget() {

    override val stateDefinition = PreferencesGlanceStateDefinition

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            Content()
        }
    }

    @Composable
    private fun Content() {
        val context = LocalContext.current
        val glanceId = LocalGlanceId.current // 현재 GlanceId 가져오기
        val coroutineScope = rememberCoroutineScope() // 코루틴 스코프

        // appWidgetId를 비동기적으로 가져오기 위한 상태
        var appWidgetId by remember { mutableStateOf(AppWidgetManager.INVALID_APPWIDGET_ID) }

        // GlanceId가 변경될 때마다 appWidgetId를 가져옴
        LaunchedEffect(glanceId) {
            coroutineScope.launch {
                try {
                    appWidgetId = GlanceAppWidgetManager(context).getAppWidgetId(glanceId)
                } catch (e: Exception) {
                    // Log.e("MetronomeWidget", "Failed to get appWidgetId for $glanceId", e)
                    // 오류 처리 (예: 기본값 유지 또는 사용자에게 알림)
                }
            }
        }

        val currentBpm =
            currentState(key = MetronomeWidgetState.BPM_KEY) ?: MetronomeWidgetState.DEFAULT_BPM
        val isPlaying = currentState(key = MetronomeWidgetState.IS_PLAYING_KEY)
            ?: MetronomeWidgetState.DEFAULT_IS_PLAYING

        Column(
            modifier = GlanceModifier.fillMaxSize().padding(16.dp),
            verticalAlignment = Alignment.Vertical.CenterVertically,
            horizontalAlignment = Alignment.Horizontal.CenterHorizontally
        ) {
            Text(
                text = "$currentBpm BPM", style = TextStyle(
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = ColorProvider(Color.Black)
                )
            )
            Row(
                verticalAlignment = Alignment.Vertical.CenterVertically,
                modifier = GlanceModifier.padding(top = 0.dp),
            ) {
                Button(
                    text = "-",
                    onClick = actionSendBroadcast(
                        Intent(
                            context,
                            MetronomeWidgetReceiver::class.java
                        ).apply {
                            action = MetronomeWidgetReceiver.ACTION_DECREMENT_BPM
                            // appWidgetId가 유효할 때만 Intent에 추가
                            if (appWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
                                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                            }
                        }),
                    colors = ButtonDefaults.buttonColors(backgroundColor = ColorProvider(Color.LightGray)),
                    enabled = appWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID // appWidgetId 로드 전까지 비활성화
                )
                Spacer(modifier = GlanceModifier.width(16.dp))

                Button(
                    text = if (isPlaying) "Stop" else "Start",
                    onClick = actionSendBroadcast(
                        Intent(context, MetronomeWidgetReceiver::class.java).apply {
                            action = MetronomeWidgetReceiver.ACTION_TOGGLE_PLAYBACK
                            if (appWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
                                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                            }
                        }), colors = ButtonDefaults.buttonColors(
                        backgroundColor = ColorProvider(if (isPlaying) Color.Red else Color.Green)
                    ), enabled = appWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID
                )

                Spacer(modifier = GlanceModifier.width(16.dp))
                Button(
                    text = "+",
                    onClick = actionSendBroadcast(
                        Intent(
                            context,
                            MetronomeWidgetReceiver::class.java
                        ).apply {
                            action = MetronomeWidgetReceiver.ACTION_INCREMENT_BPM
                            if (appWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
                                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                            }
                        }),
                    colors = ButtonDefaults.buttonColors(backgroundColor = ColorProvider(Color.LightGray)),
                    enabled = appWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID
                )
            }
        }
    }
}