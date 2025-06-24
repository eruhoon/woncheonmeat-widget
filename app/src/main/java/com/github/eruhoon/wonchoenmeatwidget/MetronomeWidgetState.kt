package com.github.eruhoon.wonchoenmeatwidget

import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey

object MetronomeWidgetState {
    val BPM_KEY = intPreferencesKey("metronome_bpm") // 키 이름 변경 가능
    const val DEFAULT_BPM = 120
    val IS_PLAYING_KEY = booleanPreferencesKey("metronome_is_playing") // 서비스 실행 상태 키
    const val DEFAULT_IS_PLAYING = false
}