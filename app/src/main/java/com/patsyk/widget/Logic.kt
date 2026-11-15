package com.patsyk.widget

import java.util.Calendar
import kotlin.random.Random

/** Что сейчас происходит: занятие, приписка к подписи, прогресс этапа (0..1) */
data class MoodNow(val mood: String, val note: String = "", val progress: Double = 0.0)

/** Что Пацык сейчас делает */
object Logic {
    const val ACTION_MINUTES = 5L         // сколько длится действие из меню
    const val LIGHT_MANUAL_MINUTES = 15L  // сколько держится свет, включённый/выключенный тобой
    const val HUNGRY_AFTER_HOURS = 8.0    // через сколько часов без еды голодает
    const val REFRESH_MINUTES = 1L        // занятие меняется раз в минуту
    const val LIGHT_BLOCK_MINUTES = 10L   // раз в 10 минут решает про свет
    const val DARK_CHANCE_DAY = 0.15      // шанс выключить свет днём
    const val DARK_CHANCE_EVENING = 0.35  // и вечером (19:00–00:00)

    const val HOOK_CHANCE = 0.35          // шанс подсечь рыбу по нажатию
    const val AUTO_CATCH_CHANCE = 0.08    // шанс, что за минуту рыбалки поймает сам
    const val AUTO_CATCH_PAUSE_MINUTES = 30L // после улова сам не ловит хотя бы столько
    const val FISH_MORE_MINUTES = 3L      // после нажатия на прорубь рыбачит ещё столько

    /** История после улова: этап → длительность в минутах */
    val STORY = listOf("catch" to 1L, "eatfish" to 1L, "drink" to 3L, "full" to 5L)

    private const val MIN = 60_000L

    val STATUS = mapOf(
        "sleep" to "спит",
        "sleepy" to "клюёт носом",
        "fish" to "рыбачит · жми на прорубь",
        "eat" to "ест",
        "happy" to "доволен",
        "hungry" to "хочет есть",
        "idle" to "думает о своём",
        "dark" to "сидит в темноте",
        "catch" to "поймал рыбу!",
        "eatfish" to "ест свою рыбу",
        "drink" to "выпивает озеро",
        "full" to "наелся и выпил озеро",
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

    fun manualLight(s: State, t: Long) = s.lightAt > 0 && t - s.lightAt < LIGHT_MANUAL_MINUTES * MIN

    fun isDark(s: State, t: Long) = if (manualLight(s, t)) s.lightOff else autoDark(t)

    /** Текущий этап истории с рыбой (или null) и прогресс этапа 0..1 */
    fun storyStage(s: State, now: Long): Pair<String, Double>? {
        if (s.storyAt <= 0L) return null
        var t = now - s.storyAt
        if (t < 0) return null
        for ((stage, minutes) in STORY) {
            val d = minutes * MIN
            if (t < d) return stage to t.toDouble() / d
            t -= d
        }
        return null
    }

    fun pickMood(s: State, now: Long): MoodNow {
        storyStage(s, now)?.let { (stage, p) -> return MoodNow(stage, "", p) }

        val act = s.action
        if (act != null && now - s.actionAt < ACTION_MINUTES * MIN) return MoodNow(act)

        if (now < s.fishUntil) {
            val missNote = if (now - s.missAt < MIN) "miss" else ""
            return MoodNow("fish", missNote)
        }

        val h = hour(now)
        if (h < 7) return MoodNow("sleep")
        if (isDark(s, now)) return MoodNow("dark")
        if ((now - s.fedAt) / 3_600_000.0 > HUNGRY_AFTER_HOURS) return MoodNow("hungry")
        if (h >= 22 || h == 7) return MoodNow("sleepy")

        val prev = now - LIGHT_BLOCK_MINUTES * MIN
        val note = if (!manualLight(s, now) && autoDark(prev) && hour(prev) >= 7) "включил свет · " else ""

        val r = rng(now / (REFRESH_MINUTES * MIN))()
        val mood = if (h in 10..18) {
            if (r < 0.45) "fish" else if (r < 0.8) "idle" else "happy"
        } else {
            if (r < 0.25) "fish" else if (r < 0.8) "idle" else "happy"
        }
        return MoodNow(mood, note)
    }

    /** Поймал рыбу — запускаем историю */
    fun startCatch(s: State, now: Long) {
        s.storyAt = now
        s.fishCount = s.fishCount + 1
        s.fedAt = now          // рыба — это еда
        s.fishUntil = 0L
        s.action = null
    }

    /** Рыбачит сам: иногда ловит без тебя (решается раз в минуту) */
    fun autoCatch(s: State, now: Long): Boolean =
        now - s.storyAt > AUTO_CATCH_PAUSE_MINUTES * MIN && rng(now / MIN * 31 + 7)() < AUTO_CATCH_CHANCE

    /** Ты нажал на прорубь */
    fun hook(s: State, now: Long) {
        if (storyStage(s, now) != null) return
        if (Random.nextDouble() < HOOK_CHANCE) {
            startCatch(s, now)
        } else {
            s.missAt = now
            s.fishUntil = now + FISH_MORE_MINUTES * MIN  // не уходит — можно попробовать ещё
        }
    }
}
