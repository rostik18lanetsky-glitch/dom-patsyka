package com.patsyk.widget

import java.util.Calendar

/** Что Пацык сейчас делает — та же логика, что и в версии для iPhone */
object Logic {
    const val ACTION_MINUTES = 40L        // сколько длится действие из меню
    const val HUNGRY_AFTER_HOURS = 8.0    // через сколько часов без еды голодает
    const val REFRESH_MINUTES = 15L
    const val LIGHT_BLOCK_MINUTES = 30L   // раз в полчаса решает про свет
    const val DARK_CHANCE_DAY = 0.15      // шанс выключить свет днём
    const val DARK_CHANCE_EVENING = 0.35  // и вечером (19:00–00:00)

    private const val MIN = 60_000L

    val STATUS = mapOf(
        "sleep" to "спит",
        "sleepy" to "клюёт носом",
        "fish" to "рыбачит",
        "eat" to "ест",
        "happy" to "доволен",
        "hungry" to "хочет есть",
        "idle" to "думает о своём",
        "dark" to "сидит в темноте",
    )

    /** mulberry32 — псевдослучайные числа от «зерна» */
    fun rng(seed0: Long): () -> Double {
        var seed = seed0.toInt()
        return {
            seed += 0x6D2B79F5
            var t = seed
            t = (t xor (t ushr 15)) * (1 or t)
            t = (t + ((t xor (t ushr 7)) * (61 or t))) xor t
            ((t xor (t ushr 14)).toLong() and 0xFFFFFFFFL).toDouble() / 4294967296.0
        }
    }

    fun hour(t: Long): Int = Calendar.getInstance().apply { timeInMillis = t }.get(Calendar.HOUR_OF_DAY)

    /** Сам решает, сидеть ли в темноте */
    fun autoDark(t: Long): Boolean {
        val h = hour(t)
        if (h < 7) return false
        val block = t / (LIGHT_BLOCK_MINUTES * MIN)
        val chance = if (h >= 19) DARK_CHANCE_EVENING else DARK_CHANCE_DAY
        return rng(block * 13 + 5)() < chance
    }

    fun manualLight(s: State, t: Long) = s.lightAt > 0 && t - s.lightAt < ACTION_MINUTES * MIN

    fun isDark(s: State, t: Long) = if (manualLight(s, t)) s.lightOff else autoDark(t)

    /** Возвращает (занятие, приписка «включил свет · » или пусто) */
    fun pickMood(s: State, now: Long): Pair<String, String> {
        val act = s.action
        if (act != null && now - s.actionAt < ACTION_MINUTES * MIN) return act to ""

        val h = hour(now)
        if (h < 7) return "sleep" to ""
        if (isDark(s, now)) return "dark" to ""
        if ((now - s.fedAt) / 3_600_000.0 > HUNGRY_AFTER_HOURS) return "hungry" to ""
        if (h >= 22 || h == 7) return "sleepy" to ""

        val prev = now - LIGHT_BLOCK_MINUTES * MIN
        val note = if (!manualLight(s, now) && autoDark(prev) && hour(prev) >= 7) "включил свет · " else ""

        val r = rng(now / (REFRESH_MINUTES * MIN))()
        val mood = if (h in 10..18) {
            if (r < 0.45) "fish" else if (r < 0.8) "idle" else "happy"
        } else {
            if (r < 0.25) "fish" else if (r < 0.8) "idle" else "happy"
        }
        return mood to note
    }
}
