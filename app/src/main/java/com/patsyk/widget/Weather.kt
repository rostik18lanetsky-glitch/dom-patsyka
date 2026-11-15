package com.patsyk.widget

import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale

/** Погода с Open-Meteo (бесплатно, без ключа) */
object Weather {
    const val WEATHER_MINUTES = 30L

    val LABEL = mapOf(
        "clear" to "ясно", "cloudy" to "облачно", "rain" to "дождь",
        "snow" to "снег", "fog" to "туман", "storm" to "гроза",
    )

    fun kind(code: Int): String = when {
        code <= 1 -> "clear"
        code <= 3 -> "cloudy"
        code == 45 || code == 48 -> "fog"
        code in 71..77 || code == 85 || code == 86 -> "snow"
        code >= 95 -> "storm"
        else -> "rain" // морось, дождь, ливни
    }

    /** Берёт погоду из кэша или скачивает (не вызывать в главном потоке) */
    fun get(s: State, now: Long): WeatherNow? {
        val cached = s.weather()
        if (cached != null && now - s.weatherAt < WEATHER_MINUTES * 60_000L) return cached
        try {
            val url = String.format(
                Locale.US,
                "https://api.open-meteo.com/v1/forecast?latitude=%.3f&longitude=%.3f&current=temperature_2m,weather_code,is_day&timezone=auto",
                s.lat, s.lon
            )
            val c = URL(url).openConnection() as HttpURLConnection
            c.connectTimeout = 8000
            c.readTimeout = 8000
            val body = c.inputStream.bufferedReader().use { it.readText() }
            c.disconnect()
            val cur = JSONObject(body).getJSONObject("current")
            val w = WeatherNow(
                cur.getDouble("temperature_2m"),
                kind(cur.getInt("weather_code")),
                cur.getInt("is_day") == 1
            )
            s.saveWeather(now, w)
            return w
        } catch (e: Exception) {
            return cached // нет интернета — прошлая погода
        }
    }
}
