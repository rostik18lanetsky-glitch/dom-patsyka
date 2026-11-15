// Генерирует кадры анимации Пацыка для iPhone (виджет в приложении Scripting).
// Рисует тот же Renderer, что и Android-виджет, — поэтому Пацык на обоих телефонах одинаковый.
import com.patsyk.widget.Renderer
import com.patsyk.widget.WeatherNow
import java.awt.image.BufferedImage
import java.io.File
import java.util.Calendar
import javax.imageio.ImageIO

const val FRAMES = 10
const val SCALE = 1.25f

data class Ch(val key: String, val mood: String, val p: Double = 0.0, val wk: String = "cloudy")

/** Вырезать кусок и сохранить без прозрачности (меньше весит) */
fun save(src: BufferedImage, x: Int, y: Int, w: Int, h: Int, file: File) {
    val out = BufferedImage(w, h, BufferedImage.TYPE_INT_RGB)
    val g = out.createGraphics()
    g.color = java.awt.Color(0x1e, 0x1e, 0x20)
    g.fillRect(0, 0, w, h)
    g.drawImage(src.getSubimage(x, y, w, h), 0, 0, null)
    g.dispose()
    ImageIO.write(out, "png", file)
}

fun main(args: Array<String>) {
    val out = File(args.getOrElse(0) { "out" })
    val now = Calendar.getInstance().apply { set(2026, 10, 15, 14, 10, 0) }.timeInMillis
    fun px(v: Int) = (v * SCALE).toInt()

    val chars = listOf(
        Ch("idle", "idle"), Ch("idle_storm", "idle", wk = "storm"), Ch("idle_rain", "idle", wk = "rain"),
        Ch("idle_snow", "idle", wk = "snow"), Ch("happy", "happy"), Ch("eat", "eat"), Ch("sleepy", "sleepy"),
        Ch("sleep", "sleep"), Ch("fish", "fish"), Ch("catch", "catch"), Ch("eatfish", "eatfish"),
        Ch("drink0", "drink", 0.0), Ch("drink1", "drink", 0.45), Ch("drink2", "drink", 0.85), Ch("full", "full"),
        Ch("lampreach", "lampreach"), Ch("dark", "dark"), Ch("read", "read"), Ch("music", "music"),
        Ch("dance", "dance"), Ch("tea", "tea"), Ch("paint0", "paint", 0.0), Ch("paint1", "paint", 1.0),
        Ch("exercise", "exercise"), Ch("meditate", "meditate"), Ch("bubbles", "bubbles"), Ch("juggle", "juggle"),
        Ch("plant0", "plant", 0.0), Ch("plant1", "plant", 1.0), Ch("sneeze0", "sneeze", 0.0),
        Ch("sneeze1", "sneeze", 1.0), Ch("guitar", "guitar"), Ch("phone", "phone"), Ch("ball", "ball"),
    )

    // Пацык с предметами: вся ширина сцены, y 0..262 (окно ляжет поверх)
    Renderer.layer = 1
    for (c in chars) for (hat in listOf(false, true)) {
        val w = WeatherNow(if (hat) -5.0 else 10.0, c.wk, true)
        val dir = File(out, "c/${c.key}${if (hat) "_hat" else ""}").apply { mkdirs() }
        for (f in 0 until FRAMES) {
            val img = Renderer.draw(c.mood, "", now, w, now, f, FRAMES, SCALE, c.p, 0).img
            save(img, 0, 0, px(680), px(262), File(dir, "$f.png"))
        }
    }

    // Окно с погодой: область x 22..165, y 64..186 сцены
    Renderer.layer = 2
    for (k in listOf("clear", "cloudy", "rain", "snow", "fog", "storm")) for (day in listOf(true, false)) {
        val dir = File(out, "w/${k}_${if (day) "d" else "n"}").apply { mkdirs() }
        for (f in 0 until FRAMES) {
            val img = Renderer.draw("idle", "", now, WeatherNow(10.0, k, day), now, f, FRAMES, SCALE, 0.0, 0).img
            save(img, px(22), px(64), px(143), px(122), File(dir, "$f.png"))
        }
    }
    Renderer.layer = 0

    File(out, "version.txt").writeText((System.getenv("GITHUB_SHA") ?: "local").take(12))
    println("Готово: ${chars.size * 2} наборов Пацыка и 12 наборов окна по $FRAMES кадров")
}
