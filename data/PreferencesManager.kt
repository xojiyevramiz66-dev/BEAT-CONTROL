package com.beatcontrol.app.data

import android.content.Context
import android.content.SharedPreferences

class PreferencesManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("beat_control_prefs", Context.MODE_PRIVATE)

    var soundVolume: Float
        get() = prefs.getFloat("sound_volume", 1.0f)
        set(value) = prefs.edit().putFloat("sound_volume", value).apply()

    var musicVolume: Float
        get() = prefs.getFloat("music_volume", 1.0f)
        set(value) = prefs.edit().putFloat("music_volume", value).apply()

    var sensitivity: Float
        get() = prefs.getFloat("sensitivity", 1.0f)
        set(value) = prefs.edit().putFloat("sensitivity", value).apply()

    fun getHighScore(level: String): Int {
        return prefs.getInt("high_score_$level", 0)
    }

    fun saveHighScore(level: String, score: Int): Boolean {
        val current = getHighScore(level)
        if (score > current) {
            prefs.edit().putInt("high_score_$level", score).apply()
            return true
        }
        return false
    }
}
