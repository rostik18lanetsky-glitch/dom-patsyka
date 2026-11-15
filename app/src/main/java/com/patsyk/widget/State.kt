package com.patsyk.widget

import android.content.Context

/** Погода: температура, вид (clear/cloudy/rain/snow/fog/storm), день или ночь */
data class WeatherNow(val t: Double, val kind: String, val day: Boolean)

/** Всё, что Пацык помнит между обновлениями (хранится в SharedPreferences) */
class State(ctx: Context) {
    private val p = ctx.getSharedPreferences("patsyk", Context.MODE_PRIVATE)

    init {
        if (!p.contains("fedAt")) p.edit().putLong("fedAt", System.currentTimeMillis()).apply()
    }

    var fedAt: Long
        get() = p.getLong("fedAt", 0L)
        set(v) = p.edit().putLong("fedAt", v).apply()

    var action: String?
        get() = p.getString("action", null)
        set(v) = p.edit().putString("action", v).apply()

    var actionAt: Long
        get() = p.getLong("actionAt", 0L)
        set(v) = p.edit().putLong("actionAt", v).apply()

    var lightOff: Boolean
        get() = p.getBoolean("lightOff", false)
        set(v) = p.edit().putBoolean("lightOff", v).apply()

    var lightAt: Long
        get() = p.getLong("lightAt", 0L)
        set(v) = p.edit().putLong("lightAt", v).apply()

    // рыбалка и история «поймал → съел → выпил озеро»
    var storyAt: Long
        get() = p.getLong("storyAt", 0L)
        set(v) = p.edit().putLong("storyAt", v).apply()

    var fishCount: Int
        get() = p.getInt("fishCount", 0)
        set(v) = p.edit().putInt("fishCount", v).apply()

    var fishUntil: Long
        get() = p.getLong("fishUntil", 0L)
        set(v) = p.edit().putLong("fishUntil", v).apply()

    var missAt: Long
        get() = p.getLong("missAt", 0L)
        set(v) = p.edit().putLong("missAt", v).apply()

    // координаты (последние известные)
    val hasLoc: Boolean get() = p.contains("lat")
    val lat: Double get() = p.getFloat("lat", 50.45f).toDouble()
    val lon: Double get() = p.getFloat("lon", 30.52f).toDouble()
    fun setLoc(lat: Double, lon: Double) {
        p.edit().putFloat("lat", lat.toFloat()).putFloat("lon", lon.toFloat()).putLong("wAt", 0L).apply()
    }

    // погода (кэш)
    val weatherAt: Long get() = p.getLong("wAt", 0L)
    fun weather(): WeatherNow? {
        if (!p.contains("wKind")) return null
        return WeatherNow(
            p.getFloat("wT", 0f).toDouble(),
            p.getString("wKind", "cloudy") ?: "cloudy",
            p.getBoolean("wDay", true)
        )
    }
    fun saveWeather(at: Long, w: WeatherNow) {
        p.edit().putLong("wAt", at).putFloat("wT", w.t.toFloat())
            .putString("wKind", w.kind).putBoolean("wDay", w.day).apply()
    }
}
