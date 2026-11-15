package com.patsyk.widget

import java.util.Calendar
import kotlin.random.Random

/** Что сейчас на экране: занятие, приписка к подписи, прогресс (0..1) */
data class MoodNow(val mood: String, val note: String = "", val progress: Double = 0.0)

/** Один шаг эпизода: занятие и сколько секунд длится */
data class Step(val mood: String, val sec: Int, val progress: Double = 0.0, val note: String = "")

/**
 * Пацык полностью самостоятельный: живёт «эпизодами».
 * Каждый эпизод — несколько шагов; обычный эпизод длится 15 секунд.
 */
object Logic {
    const val STEP_SEC = 15                 // занятие меняется каждые 15 секунд
    const val CATCH_CHANCE = 0.45           // шанс, что рыбалка закончится уловом

    val STATUS = mapOf(
        "sleep" to "спит",
        "sleepy" to "клюёт носом",
        "fish" to "рыбачит",
        "eat" to "перекусывает",
        "happy" to "доволен",
        "idle" to "думает о своём",
        "dark" to "сидит в темноте",
        "catch" to "поймал рыбу!",
        "eatfish" to "ест свою рыбу",
        "drink" to "выпивает озеро",
        "full" to "наелся и выпил озеро",
        "lampreach" to "тянется к лампочке",
        "read" to "читает книжку",
        "music" to "слушает музыку",
        "dance" to "танцует",
        "tea" to "пьёт чай",
        "paint" to "рисует картину",
        "exercise" to "качается",
        "meditate" to "медитирует",
        "bubbles" to "пускает пузыри",
        "juggle" to "жонглирует",
        "plant" to "поливает цветок",
        "sneeze" to "сейчас чихнёт…",
        "guitar" to "играет на гитаре",
        "phone" to "сидит в телефоне",
        "ball" to "играет с мячиком",
    )

    /** Из каких шагов состоит эпизод */
    fun steps(type: String): List<Step> = when (type) {
        "fish_ok" -> listOf(
            Step("fish", STEP_SEC),
            Step("catch", 8),                  // поймал!
            Step("eatfish", 10),               // съел
            Step("drink", 5, 0.0),             // выпил озеро за 15 секунд
            Step("drink", 5, 0.45),
            Step("drink", 5, 0.85),
            Step("full", STEP_SEC),            // объелся
        )
        "fish_fail" -> listOf(
            Step("fish", STEP_SEC),
            Step("fish", 6, note = "miss"),    // сорвалась
        )
        "lamp" -> listOf(
            Step("lampreach", 6),              // тянется к шнурку
            Step("dark", STEP_SEC, note = "off"), // щёлк — темно
        )
        "paint" -> listOf(
            Step("paint", 10, 0.0),            // рисует
            Step("paint", 6, 1.0),             // готово — шедевр!
        )
        "plant" -> listOf(
            Step("plant", 10, 0.0),            // поливает
            Step("plant", 6, 1.0),             // цветок вырос
        )
        "sneeze" -> listOf(
            Step("sneeze", 4, 0.0),            // а-а-а…
            Step("sneeze", 5, 1.0),            // апчхи!
        )
        else -> listOf(Step(type, STEP_SEC))   // остальные занятия — по 15 секунд
    }

    /** mulberry32 — псевдослучайные числа от «зерна» (для мелких деталей рисунка) */
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

    /** Сам выбирает, чем заняться дальше */
    fun chooseEpisode(now: Long, prev: String?): String {
        val h = hour(now)
        if (h < 7) return "sleep"
        val weights = mutableListOf(
            "fish" to 0.20, "idle" to 0.09, "happy" to 0.05, "lamp" to 0.07, "eat" to 0.04, "sleepy" to 0.03,
            "read" to 0.04, "music" to 0.04, "dance" to 0.035, "tea" to 0.035, "paint" to 0.04,
            "exercise" to 0.035, "meditate" to 0.03, "bubbles" to 0.035, "juggle" to 0.035,
            "plant" to 0.035, "sneeze" to 0.025, "guitar" to 0.035, "phone" to 0.03, "ball" to 0.035,
        )
        if (h >= 22 || h == 7) { // вечером и рано утром — спокойнее: клюёт носом, читает, пьёт чай
            weights.replaceAll { (k, w) ->
                k to when (k) {
                    "sleepy" -> 0.25
                    "read", "tea", "meditate" -> w * 2
                    "fish", "exercise", "dance", "juggle", "ball" -> w * 0.5
                    else -> w
                }
            }
        }
        // не повторяет то же занятие два раза подряд (кроме рыбалки)
        val prevBase = if (prev != null && prev.startsWith("fish")) "fish" else prev
        if (prevBase != null && prevBase != "fish") weights.removeAll { it.first == prevBase }
        val total = weights.sumOf { it.second }
        var r = Random.nextDouble() * total
        var pick = weights.last().first
        for ((k, w) in weights) { if (r < w) { pick = k; break }; r -= w }
        if (pick == "fish") pick = if (Random.nextDouble() < CATCH_CHANCE) "fish_ok" else "fish_fail"
        return pick
    }

    /**
     * Что сейчас происходит и когда это закончится.
     * Если эпизод закончился (или экран долго был выключен) — начинается новый.
     */
    fun current(s: State, now: Long): Pair<MoodNow, Long> {
        val type = s.epType
        val start = s.epStart
        if (type != null && now >= start) {
            var t = start
            val list = steps(type)
            for ((i, step) in list.withIndex()) {
                val end = t + step.sec * 1000L
                if (now < end) return decorate(s, step, i, now) to end
                t = end
            }
        }
        // новый эпизод
        val newType = chooseEpisode(now, type)
        s.epPrev = type
        s.epType = newType
        s.epStart = now
        s.epFishCounted = false
        val first = steps(newType)[0]
        return decorate(s, first, 0, now) to now + first.sec * 1000L
    }

    private fun decorate(s: State, step: Step, index: Int, now: Long): MoodNow {
        if (step.mood == "catch" && !s.epFishCounted) {   // считаем улов один раз
            s.fishCount = s.fishCount + 1
            s.epFishCounted = true
        }
        val note = when {
            step.note.isNotEmpty() -> step.note
            index == 0 && s.epPrev == "lamp" && hour(now) >= 7 -> "включил свет · "
            else -> ""
        }
        return MoodNow(step.mood, note, step.progress)
    }
}
