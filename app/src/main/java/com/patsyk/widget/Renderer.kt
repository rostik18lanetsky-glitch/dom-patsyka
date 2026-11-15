package com.patsyk.widget

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Typeface
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.PI

/**
 * Рисует сцену «Дом Пацыка» в картинку 680×320 (×2 для чёткости).
 * Координаты те же, что и в версии для Scriptable.
 */
object Renderer {
    private const val W = 680f
    private const val H = 320f

    private fun withAlpha(c: Int, a: Double): Int =
        Color.argb((a.coerceIn(0.0, 1.0) * 255).roundToInt(), Color.red(c), Color.green(c), Color.blue(c))

    private fun col(hex: String, a: Double = 1.0): Int {
        val c = Color.parseColor(hex)
        return Color.argb((a * 255).roundToInt(), Color.red(c), Color.green(c), Color.blue(c))
    }

    private val BG = col("#1e1e20")
    private val FLOOR = col("#18181a")
    private val TITLE = col("#8a8a90")
    private val STATUS_C = col("#c9c9cf")
    private val BODY = col("#6a6a70")
    private val BODY_HI = col("#7c7c83")
    private val OUTLINE = col("#3a3a3e")
    private val SHADOW = col("#111113")
    private val WHITE = col("#f4f4f4")
    private val BLACK = col("#151515")
    private val ORANGE = col("#f5a524")
    private val ORANGE2 = col("#d9821a")
    private val WATER = col("#3d9be9")
    private val WATER2 = col("#8fcaff")
    private val ROD = col("#c98a4b")
    private val LINE = col("#d8d8d8")
    private val RED = col("#e5484d")
    private val PINK = col("#ff7a9a", 0.55)
    private val MOON = col("#f2e6a6")
    private val BULB_ON = col("#ffe48a")
    private val GLOW = col("#ffd95a", 0.10)
    private val BULB_OFF = col("#4a4a4e")
    private val CORD = col("#0e0e10")
    private val SOCKET = col("#3a3a3e")
    private val CANDLE = col("#efefef")
    private val FRAME = col("#2e2e32")
    private val SKY_DAY = col("#5aa9e6")
    private val SKY_GREY = col("#7b8794")
    private val SKY_RAIN = col("#56616f")
    private val SKY_NIGHT = col("#141a30")
    private val SUN = col("#ffd23f")
    private val CLOUD = col("#e9edf2")
    private val CLOUD_DK = col("#8a929e")
    private val RAIN = col("#a9d4ff")
    private val SNOW = col("#ffffff")
    private val FOG = col("#dfe3e8", 0.45)
    private val HAT = col("#d64545")
    private val HAT_BAND = col("#f1f1f1")
    private val EMPTY = col("#3a3a3e")
    private val FISH = col("#8fb8de")
    private val FISH_DK = col("#5d8db8")
    private val STRAW = col("#ff6f91")
    private val DRY = col("#121214")
    private val CRACK = col("#2c2c30")
    private val PULL = col("#9a9aa0")
    private val DARKNESS = col("#000000", 0.86)

    private lateinit var cv: Canvas

    // ---- состояние анимации текущего кадра ----
    private var frame = 0
    private var frames = 1
    private var ph = 0.0          // фаза 0..2π
    private var bob = 0.0         // «дыхание» — смещение тела по вертикали
    private var blink = false     // кадр, где Пацык моргает
    private val t01: Double get() = frame.toDouble() / frames  // 0..1 по циклу
    private val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val stroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }
    private val txt = Paint(Paint.ANTI_ALIAS_FLAG)
    private val medium: Typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)

    private val Number.f: Float get() = this.toFloat()

    // ---------- примитивы ----------
    private class Pt(val x: Double, val y: Double)

    private class Pth {
        val p = Path()
        fun m(x: Number, y: Number) = p.moveTo(x.f, y.f)
        fun l(x: Number, y: Number) = p.lineTo(x.f, y.f)
        /** как addQuadCurve(point, control) в Scriptable */
        fun q(x: Number, y: Number, cx: Number, cy: Number) = p.quadTo(cx.f, cy.f, x.f, y.f)
        /** как addCurve(point, c1, c2) в Scriptable */
        fun c(x: Number, y: Number, c1x: Number, c1y: Number, c2x: Number, c2y: Number) =
            p.cubicTo(c1x.f, c1y.f, c2x.f, c2y.f, x.f, y.f)
        fun close() = p.close()
    }

    private fun fillP(p: Pth, color: Int) { fill.color = color; cv.drawPath(p.p, fill) }
    private fun strokeP(p: Pth, color: Int, w: Number) { stroke.color = color; stroke.strokeWidth = w.f; cv.drawPath(p.p, stroke) }
    private fun circle(x: Number, y: Number, r: Number, color: Int) { fill.color = color; cv.drawCircle(x.f, y.f, r.f, fill) }
    private fun oval(x: Number, y: Number, rx: Number, ry: Number, color: Int) {
        fill.color = color
        cv.drawOval(x.f - rx.f, y.f - ry.f, x.f + rx.f, y.f + ry.f, fill)
    }
    private fun rect(x: Number, y: Number, w: Number, h: Number, color: Int) {
        fill.color = color
        cv.drawRect(x.f, y.f, x.f + w.f, y.f + h.f, fill)
    }
    private fun line(x1: Number, y1: Number, x2: Number, y2: Number, color: Int, w: Number) {
        stroke.color = color; stroke.strokeWidth = w.f
        cv.drawLine(x1.f, y1.f, x2.f, y2.f, stroke)
    }
    private fun strokeOval(x: Number, y: Number, rx: Number, ry: Number, color: Int, w: Number) {
        stroke.color = color; stroke.strokeWidth = w.f
        cv.drawOval(x.f - rx.f, y.f - ry.f, x.f + rx.f, y.f + ry.f, stroke)
    }
    private fun strokeCircle(x: Number, y: Number, r: Number, color: Int, w: Number) {
        stroke.color = color; stroke.strokeWidth = w.f
        cv.drawCircle(x.f, y.f, r.f, stroke)
    }
    /** текст: (x, y) — левый верхний угол, как drawTextInRect */
    private fun text(s: String, x: Number, y: Number, size: Number, bold: Boolean, color: Int) {
        txt.textSize = size.f
        txt.typeface = if (bold) Typeface.DEFAULT_BOLD else medium
        txt.color = color
        cv.drawText(s, x.f, y.f - txt.fontMetrics.ascent, txt)
    }

    // ---------- Пацык ----------
    private fun blobPath(cx: Double, cy: Double, rx: Double, ry: Double, squash: Double): Pth {
        val k = 0.56
        val top = cy - ry * (1 - squash)
        val bot = cy + ry
        val l = cx - rx * (1 + squash * 0.3)
        val r = cx + rx * (1 + squash * 0.3)
        return Pth().apply {
            m(cx + 6, top)
            c(r, cy + 4, cx + 6 + rx * k, top, r, cy - ry * k + 4)
            c(cx - 4, bot, r, cy + ry * k + 2, cx - 4 + rx * 0.62, bot)
            c(l, cy + 10, cx - 4 - rx * 0.62, bot, l, cy + ry * k + 6)
            c(cx + 6, top, l, cy - ry * k + 4, cx + 6 - rx * k, top)
            close()
        }
    }

    private fun drawBody(cx: Double, cy: Double, squash: Double, k: Double = 1.0) {
        val rx = 82.0 * k; val ry = 76.0 * k
        oval(cx, cy - bob + ry + 2, rx * (1.05 - bob * 0.004), 11, SHADOW) // тень стоит на полу
        fillP(blobPath(cx, cy, rx, ry, squash), BODY)
        strokeP(blobPath(cx, cy, rx, ry, squash), OUTLINE, 5)
        oval(cx - 30, cy - ry * 0.45, 18, 9, BODY_HI) // блик
    }

    private fun eye(x: Double, y: Double, kind: String, look: Pt) {
        if (kind == "closed") {
            strokeP(Pth().apply { m(x - 13, y); q(x + 13, y, x, y + 10) }, BLACK, 4.5); return
        }
        if (kind == "happy") {
            strokeP(Pth().apply { m(x - 13, y + 4); q(x + 13, y + 4, x, y - 12) }, BLACK, 4.5); return
        }
        circle(x, y, 15, WHITE)
        strokeCircle(x, y, 15, BLACK, 2.5)
        circle(x + look.x * 6, y + look.y * 6, 6.5, BLACK)
        circle(x + look.x * 6 + 2, y + look.y * 6 - 2.5, 2, WHITE)
        if (kind == "half") { // веко
            fillP(Pth().apply {
                m(x - 16, y); q(x + 16, y, x, y - 26); l(x + 16, y - 17); l(x - 16, y - 17); close()
            }, BODY)
            line(x - 15, y, x + 15, y, BLACK, 3.5)
        }
    }

    private fun mouth(x: Double, y: Double, kind: String) {
        val p = Pth()
        when (kind) {
            "smile" -> { p.m(x - 16, y - 2); p.q(x + 16, y - 2, x, y + 14) }
            "frown" -> { p.m(x - 14, y + 6); p.q(x + 14, y + 6, x, y - 6) }
            "wavy" -> { p.m(x - 16, y); p.q(x, y, x - 8, y - 7); p.q(x + 16, y, x + 8, y + 7) }
            "o" -> { oval(x, y + 3, 8, 10, BLACK); return }
            "chew" -> { oval(x, y + 2, 11, 7, BLACK); circle(x - 3, y + 1, 3, ORANGE); return }
            else -> { p.m(x - 13, y); p.l(x + 13, y) }
        }
        strokeP(p, BLACK, 4.5)
    }

    private fun brows(e1: Double, e2: Double, y: Double, kind: String?) {
        if (kind == "worried") {
            line(e1 - 14, y - 22, e1 + 10, y - 28, BLACK, 4)
            line(e2 - 10, y - 28, e2 + 14, y - 22, BLACK, 4)
        } else if (kind == "up") {
            line(e1 - 12, y - 28, e1 + 10, y - 30, BLACK, 4)
            line(e2 - 10, y - 30, e2 + 12, y - 28, BLACK, 4)
        }
    }

    private fun face(
        cx: Double, cy: Double, eyes: String, mouthKind: String, look: Pt = Pt(0.0, 0.0),
        browsKind: String? = null, blush: Boolean = false
    ) {
        val ey = cy - 18; val e1 = cx - 4; val e2 = cx + 34
        val ek = if (blink && (eyes == "open" || eyes == "half")) "closed" else eyes
        eye(e1, ey, ek, look)
        eye(e2, ey, ek, look)
        brows(e1, e2, ey, browsKind)
        if (blush) { oval(e1 - 14, cy + 10, 12, 7, PINK); oval(e2 + 14, cy + 10, 12, 7, PINK) }
        mouth(cx + 15, cy + 22, mouthKind)
    }

    // ---------- предметы ----------
    private fun iceHole(x: Double, y: Double, k: Double = 1.0) {
        oval(x, y, 92, 20, DRY)                              // край проруби
        if (k > 0.02) {
            oval(x, y, 92 * k, 20 * k, WATER)
            oval(x - 10 * k, y - 4 * k, 58 * k, 9 * k, WATER2)
        }
    }

    private fun dryHole(x: Double, y: Double) {           // озеро выпито — сухо и трещины
        oval(x, y, 92, 20, DRY)
        strokeOval(x, y, 92, 20, CRACK, 2)
        line(x - 40, y - 6, x - 10, y + 2, CRACK, 2); line(x - 10, y + 2, x + 12, y - 5, CRACK, 2)
        line(x + 12, y - 5, x + 45, y + 6, CRACK, 2); line(x - 10, y + 2, x - 18, y + 12, CRACK, 2)
        oval(x + 30, y - 2, 6, 2, WATER)                     // последняя капля
    }

    private fun food(x: Double, y: Double, s: Double) {
        fillP(Pth().apply {
            m(x - 22 * s, y + 14 * s); l(x + 24 * s, y + 14 * s); l(x + 8 * s, y - 18 * s); close()
        }, ORANGE)
        circle(x + 2 * s, y + 4 * s, 4 * s, ORANGE2)
        circle(x + 12 * s, y + 8 * s, 3 * s, ORANGE2)
    }

    private fun heart(x: Double, y: Double, s: Double, color: Int) {
        fillP(Pth().apply {
            m(x, y + 12 * s)
            c(x - 14 * s, y - 2 * s, x - 6 * s, y + 6 * s, x - 14 * s, y + 4 * s)
            c(x, y - 6 * s, x - 14 * s, y - 12 * s, x - 2 * s, y - 12 * s)
            c(x + 14 * s, y - 2 * s, x + 2 * s, y - 12 * s, x + 14 * s, y - 12 * s)
            c(x, y + 12 * s, x + 14 * s, y + 4 * s, x + 6 * s, y + 6 * s)
        }, color)
    }

    /** Рыбка: (x, y) — центр, голова слева; rot — поворот в градусах */
    private fun fish(x: Double, y: Double, s: Double, rot: Double = 0.0) {
        cv.save()
        cv.rotate(rot.toFloat(), x.toFloat(), y.toFloat())
        fillP(Pth().apply { m(x + 16 * s, y); l(x + 32 * s, y - 11 * s); l(x + 30 * s, y); l(x + 32 * s, y + 11 * s); close() }, FISH_DK) // хвост
        oval(x, y, 21 * s, 10 * s, FISH)                                           // тело
        fillP(Pth().apply { m(x - 4 * s, y - 8 * s); l(x + 8 * s, y - 15 * s); l(x + 10 * s, y - 8 * s); close() }, FISH_DK) // плавник
        circle(x - 12 * s, y - 2 * s, 2.6 * s, BLACK)                              // глаз
        circle(x - 11.4 * s, y - 2.8 * s, 0.9 * s, WHITE)
        line(x - 3 * s, y - 5 * s, x - 3 * s, y + 5 * s, FISH_DK, 1.4 * s)        // жабры
        cv.restore()
    }

    /** Точка на квадратичной кривой (для соломинки) */
    private fun quadPt(x0: Double, y0: Double, cx: Double, cy: Double, x1: Double, y1: Double, t: Double): Pt {
        val a = (1 - t) * (1 - t); val b = 2 * (1 - t) * t; val c = t * t
        return Pt(a * x0 + b * cx + c * x1, a * y0 + b * cy + c * y1)
    }

    private fun cloud(x: Double, y: Double, s: Double, color: Int) {
        circle(x, y, 12 * s, color); circle(x + 14 * s, y - 6 * s, 14 * s, color)
        circle(x + 28 * s, y, 11 * s, color); rect(x, y - 2 * s, 28 * s, 13 * s, color)
    }

    private fun windowView(w: WeatherNow?, nightScene: Boolean) {
        val x = 40.0; val y = 74.0; val ww = 112.0; val hh = 96.0
        val kind = w?.kind ?: "cloudy"
        val day = w?.day ?: !nightScene
        val sky = if (day) {
            when (kind) { "clear" -> SKY_DAY; "rain", "storm" -> SKY_RAIN; else -> SKY_GREY }
        } else SKY_NIGHT
        rect(x - 7, y - 7, ww + 14, hh + 14, FRAME)
        rect(x, y, ww, hh, sky)

        if (kind == "clear") {
            if (day) circle(x + 78, y + 30, 15, SUN)
            else {
                circle(x + 78, y + 28, 12, MOON); circle(x + 84, y + 24, 11, sky)
                listOf(14 to 14, 36 to 60, 96 to 70, 50 to 26).forEach { (a, b) -> circle(x + a, y + b, 1.8, MOON) }
            }
        }
        if (kind == "cloudy") {
            if (day) circle(x + 82, y + 26, 13, SUN)
            cloud(x + 14, y + 36, 1.0, CLOUD); cloud(x + 58, y + 64, 0.8, CLOUD)
        }
        if (kind == "rain" || kind == "storm") {
            val cc = if (kind == "storm") CLOUD_DK else CLOUD
            cloud(x + 10, y + 22, 1.1, cc)
            cloud(x + 60, y + 18, 0.9, cc)
            for (i in 0 until 12) {
                val rx = x + 8 + (i * 37) % (ww - 14)
                val ry = y + 40 + (i * 23 + t01 * (hh - 52)) % (hh - 52) // капли падают
                line(rx, ry, rx - 4, ry + 10, RAIN, 2.5)
            }
            if (kind == "storm" && (frames == 1 || frame == 1 || frame == 2)) { // молния вспыхивает
                if (frames > 1 && frame == 1) rect(x, y, ww, hh, withAlpha(SUN, 0.25))
                strokeP(Pth().apply {
                    m(x + 62, y + 30); l(x + 52, y + 54); l(x + 62, y + 54); l(x + 50, y + 82)
                }, SUN, 4)
            }
        }
        if (kind == "snow") {
            for (i in 0 until 16) {
                val sx = x + 6 + (i * 41) % (ww - 10) + 2 * sin(ph + i)              // снежинки покачиваются
                val sy = y + 6 + (i * 29 + t01 * (hh - 16)) % (hh - 16)            // и падают
                circle(sx, sy, 2.6 + (i % 3) * 0.6, SNOW)
            }
        }
        if (kind == "fog") for (i in 0 until 4) rect(x, y + 14 + i * 22, ww, 10, FOG)

        // рама и подоконник
        rect(x + ww / 2 - 2.5, y, 5, hh, FRAME)
        rect(x, y + hh / 2 - 2.5, ww, 5, FRAME)
        rect(x - 14, y + hh + 6, ww + 28, 9, FRAME)
        if (kind == "snow") oval(x + ww / 2, y + hh - 2, ww / 2 - 4, 5, SNOW)

        if (w != null) {
            val t = w.t.roundToInt()
            val sign = if (t > 0) "+" else if (t < 0) "−" else ""
            text(sign + abs(t) + "° " + (Weather.LABEL[kind] ?: ""), x - 6, y + hh + 20, 21, false, TITLE)
        }
    }

    private fun hat(x: Double, top: Double) { // вязаная шапка в мороз
        fillP(Pth().apply {
            m(x - 60, top + 30); c(x + 60, top + 30, x - 54, top - 36, x + 54, top - 36); close()
        }, HAT)
        rect(x - 64, top + 20, 128, 16, HAT_BAND)
        circle(x + 52, top - 4, 11, HAT_BAND)
    }

    /** Конец шнурка-выключателя (куда тянется Пацык) */
    private fun knobX(x: Double) = x + 82
    private fun knobY(up: Double, pull: Double) = 126 - up * 0.3 + pull

    private fun lamp(x: Double, on: Boolean, up: Double, pull: Double = 0.0) {
        line(x, 0, x, 50 - up, CORD, 3)
        line(x + 8, 56 - up, knobX(x), knobY(up, pull), PULL, 2)   // шнурок-выключатель
        circle(knobX(x), knobY(up, pull) + 4, 4.5, PULL)
        rect(x - 9, 46 - up, 18, 14, SOCKET)
        if (on) {
            listOf(70, 54, 40, 28).forEach { r -> circle(x, 74 - up, r * (1 + 0.03 * sin(ph)), GLOW) }
            circle(x, 74 - up, 16, BULB_ON)
            circle(x - 5, 69 - up, 4, WHITE)
        } else {
            circle(x, 74 - up, 16, BULB_OFF)
        }
    }

    private fun candle(x: Double, y: Double) {
        val fl = 1 + 0.18 * sin(ph * 2) + 0.08 * sin(ph * 5)   // пламя дрожит
        val sway = 1.5 * sin(ph * 3)
        listOf(36, 26, 18).forEach { r -> circle(x, y - 34, r * fl, GLOW) }
        rect(x - 9, y - 22, 18, 40, CANDLE)
        line(x, y - 22, x, y - 28, BLACK, 2)
        oval(x + sway, y - 36 - (fl - 1) * 6, 6, 10 * fl, ORANGE)
        oval(x + sway * 0.6, y - 33, 3, 5 * fl, BULB_ON)
    }

    // ---------- вся сцена ----------
    @Synchronized
    fun draw(
        mood: String, note: String, now: Long, weather: WeatherNow?, fedAt: Long,
        frameIndex: Int = 0, frameCount: Int = 1, scale: Float = 2f,
        progress: Double = 0.0, fishCount: Int = 0
    ): Bitmap {
        frame = frameIndex; frames = frameCount.coerceAtLeast(1)
        ph = 2 * PI * frame / frames
        val anim = frames > 1
        blink = anim && frame == frames - 2
        bob = if (!anim) 0.0 else when (mood) {
            "sleep" -> 3 * sin(ph)            // глубоко дышит во сне
            "happy" -> -6 * abs(sin(ph))      // подпрыгивает от радости
            "dark" -> 0.0
            else -> 2 * sin(ph)
        }
        val bmp = Bitmap.createBitmap((W * scale).toInt(), (H * scale).toInt(), Bitmap.Config.ARGB_8888)
        cv = Canvas(bmp)
        cv.scale(scale, scale)
        // скруглённые углы виджета
        cv.clipPath(Path().apply { addRoundRect(RectF(0f, 0f, W, H), 40f, 40f, Path.Direction.CW) })

        rect(0, 0, W, H, BG)
        rect(0, 262, W, H - 262, FLOOR)

        val night = mood == "sleep"
        if (night) { // луна и звёзды
            circle(610, 58, 22, MOON); circle(622, 50, 20, BG)
            listOf(250 to 95, 330 to 48, 520 to 40, 215 to 205).forEach { (x, y) -> circle(x, y, 2.5, MOON) }
        }

        windowView(weather, night)

        val cx = 455.0; val cy = 182.0 + bob
        var headY = cy
        val wk = weather?.kind
        val r = Logic.rng((now / (Logic.STEP_SEC * 1000L)) * 7 + 3)
        var reach = false

        when (mood) {
            "sleep" -> {
                drawBody(cx, cy + 6, 0.12); headY = cy + 6
                face(cx, cy + 10, "closed", "o")
                val dz = t01 * 16                                      // «Z z» плывут вверх
                text("z", 528 + dz * 0.4, 128 - dz, 30, true, withAlpha(STATUS_C, 1 - t01 * 0.5))
                text("Z", 552 + dz * 0.4, 82 - dz, 42, true, withAlpha(STATUS_C, 1 - t01 * 0.5))
            }
            "sleepy" -> {
                drawBody(cx, cy, 0.05)
                face(cx, cy, "half", "wavy", Pt(0.0, 0.5))
            }
            "fish" -> {
                iceHole(225.0, 236.0)
                val tipY = 70 + 6 * sin(ph)                 // кончик удочки качается
                val bobY = 231 + 2.5 * sin(ph + 1.2)        // поплавок качается на воде
                if (anim) {                                  // круги по воде
                    val rr = 8 + t01 * 30
                    strokeOval(232, 237, rr, rr * 0.28, withAlpha(WATER2, 1 - t01), 2)
                }
                line(382, 200, 282, tipY, ROD, 6)       // удочка
                line(282, tipY, 232, bobY + 1, LINE, 2) // леска
                circle(232, bobY, 6, RED)               // поплавок
                drawBody(cx, cy, 0.0)
                line(378, 196, 398, 206, OUTLINE, 7)  // ручка
                val m = if (r() < 0.5) "flat" else "wavy"
                val b = if (r() < 0.5) "worried" else null
                face(cx, cy, "open", m, Pt(-1.0, 0.6), b)
            }
            "eat" -> {
                drawBody(cx, cy, 0.0)
                food(cx + 78, cy + 30, 0.95)
                face(cx, cy, "happy", if (anim && frame % 2 == 1) "o" else "chew", blush = true) // жуёт
                circle(cx - 70, cy + 80, 4, ORANGE); circle(cx + 92, cy + 76, 3, ORANGE)
            }
            "happy" -> {
                drawBody(cx, cy - 6, -0.05); headY = cy - 6
                face(cx, cy - 6, "happy", "smile", blush = true)
                val rise = t01 * 18                            // сердечки поднимаются
                val rise2 = ((t01 + 0.5) % 1.0) * 18
                heart(590.0, 92.0 - rise, 1.2, withAlpha(RED, 1 - t01 * 0.6))
                heart(545.0, 58.0 - rise2 + 9, 0.7, withAlpha(RED, 1 - ((t01 + 0.5) % 1.0) * 0.6))
            }
            "hungry" -> {
                drawBody(cx, cy + 4, 0.08); headY = cy + 4
                face(cx, cy + 4, "open", "frown", Pt(-0.8, -0.7), "worried")
                circle(352, 112, 7 + 1.5 * sin(ph), STATUS_C); circle(328, 90, 11 + 1.5 * sin(ph + 1), STATUS_C)
                oval(262, 80, 58, 40, STATUS_C)
                food(262.0, 78.0, 0.9)
            }
            "catch" -> { // поймал! рыба висит на леске и бьётся
                iceHole(225.0, 236.0)
                val sx = 300 + 5 * sin(ph * 2)
                line(382, 200, 300, 48, ROD, 6)                       // удочка задрана вверх
                line(300, 48, sx, 104, LINE, 2)
                fish(sx, 128.0, 1.25, 90 + 18 * sin(ph * 2))          // рыба трепыхается
                for (i in 0 until 4) {                                // брызги из проруби
                    val u = (t01 + i * 0.25) % 1.0
                    circle(205 + i * 14, 226 - 40 * u + 30 * u * u, 3.5 * (1 - u) + 1, withAlpha(WATER2, 1 - u))
                }
                drawBody(cx, cy - 4, -0.05); headY = cy - 4
                line(378, 196, 398, 206, OUTLINE, 7)
                face(cx, cy - 4, "open", "o", Pt(-1.0, -0.8), "up")
            }
            "eatfish" -> { // ест рыбу
                iceHole(225.0, 236.0)
                drawBody(cx, cy, 0.0)
                val bite = if (anim && frame % 2 == 1) 3.0 else 0.0
                fish(cx + 48 + bite, cy + 26, 1.1, -8.0)               // рыба у рта, хвост наружу
                face(cx, cy, "happy", if (anim && frame % 2 == 1) "o" else "chew", blush = true)
                circle(cx - 66, cy + 82, 3, FISH); circle(cx + 96, cy + 78, 2.5, FISH)
            }
            "drink" -> { // пьёт озеро через соломинку
                val k = (1 - 0.85 * progress).coerceAtLeast(0.15)
                iceHole(225.0, 236.0, k)
                drawBody(cx, cy, 0.04 + 0.05 * sin(ph))
                // соломинка от рта к проруби
                val x0 = cx + 6; val y0 = cy + 22; val c1x = 330.0; val c1y = 120.0; val x1 = 240.0; val y1 = 232.0
                val straw = Pth().apply { m(x0, y0); q(x1, y1, c1x, c1y) }
                strokeP(straw, STRAW, 7)
                strokeP(Pth().apply { m(x0, y0); q(x1, y1, c1x, c1y) }, withAlpha(WHITE, 0.5), 2)
                for (i in 0 until 4) {                                // вода бежит по соломинке
                    val u = 1 - ((t01 + i * 0.25) % 1.0)
                    val pt = quadPt(x0, y0, c1x, c1y, x1, y1, u)
                    circle(pt.x, pt.y, 2.6, WATER2)
                }
                face(cx, cy, "closed", "o", blush = true)
                oval(cx - 18, cy + 14, 14 + 3 * sin(ph), 10 + 2 * sin(ph), withAlpha(BODY_HI, 0.8)) // щёки надулись
                oval(cx + 52, cy + 14, 14 + 3 * sin(ph), 10 + 2 * sin(ph), withAlpha(BODY_HI, 0.8))
            }
            "full" -> { // объелся: раздулся, озеро высохло
                dryHole(225.0, 236.0)
                val k = 1.16
                val fcy = 258.0 - 76.0 * k + bob
                drawBody(cx, fcy, 0.0, k); headY = fcy - 76.0 * (k - 1)
                face(cx, fcy, "happy", "smile", blush = true)
                strokeP(Pth().apply { m(cx - 30, fcy + 52); q(cx + 50, fcy + 52, cx + 10, fcy + 66) }, withAlpha(OUTLINE, 0.6), 3) // пузико
                // косточки от рыбы
                line(318, 254, 358, 254, WHITE, 2.5)
                for (i in 0 until 4) line(326 + i * 8, 247, 326 + i * 8, 261, WHITE, 2)
                fillP(Pth().apply { m(358, 254); l(370, 246); l(370, 262); close() }, WHITE)
            }
            "lampreach" -> { // тянется к шнурку лампочки
                reach = true
                val st = -0.14 - 0.06 * abs(sin(ph))
                drawBody(cx, cy, st); headY = cy - 76 * (-st)
                face(cx, cy - 6, "open", "o", Pt(0.9, -0.8), "up")
            }
            "dark" -> {
                drawBody(cx, cy, 0.0)
                face(cx, cy, "open", "flat")
                rect(0, 0, W, H, DARKNESS)
                val lk = if (r() < 0.5) Pt(-1.0, 0.0) else Pt(1.0, -0.3)
                val ek = if (blink) "closed" else "open"  // моргнул — и глаза пропали в темноте
                eye(cx - 4, cy - 18, ek, lk) // в темноте видны только глаза
                eye(cx + 34, cy - 18, ek, lk)
                candle(cx + 92, cy + 62)
            }
            else -> { // idle
                val looks = listOf(Pt(1.0, 0.0), Pt(-1.0, 0.0), Pt(0.7, -0.7), Pt(-0.7, 0.6), Pt(0.0, 0.0))
                val mouths = listOf("flat", "wavy", "o", "flat")
                drawBody(cx, cy, 0.0)
                val lookIdx = floor(r() * looks.size).toInt()
                val mouthIdx = floor(r() * mouths.size).toInt()
                val br = r()
                when (wk) {
                    "storm" -> face(cx, cy, "open", "wavy", Pt(-1.0, -0.4), "worried")       // гроза — боится
                    "rain" -> face(cx, cy, "open", "flat", Pt(-1.0, -0.5))                    // смотрит в окно
                    "snow" -> face(cx, cy, "open", "o", Pt(-1.0, -0.5), "up")
                    else -> face(cx, cy, "open", mouths[mouthIdx], looks[lookIdx], if (br < 0.4) "up" else null)
                }
            }
        }

        val cold = weather != null && weather.t <= 0 && mood != "dark"
        if (cold) hat(cx + 6, headY - 86)
        val up = if (cold) 30.0 else 0.0
        val pull = if (reach) 9 * abs(sin(ph)) else 0.0
        lamp(455.0, !(mood == "sleep" || mood == "dark"), up, pull)
        if (reach) { // лапка тянется к шнурку
            val hx = knobX(455.0); val hy = knobY(up, pull) + 6
            val sx = cx + 58; val sy = cy - 22
            line(sx, sy, hx, hy, OUTLINE, 17)
            line(sx, sy, hx, hy, BODY, 12)
            circle(hx, hy, 9, OUTLINE); circle(hx, hy, 7, BODY)
        }

        // подписи
        text("Дом Пацыка", 34, 24, 26, true, TITLE)
        var status = Logic.STATUS[mood] ?: mood
        if (mood == "idle" && wk == "storm") status = "боится грозы"
        if (mood == "idle" && wk == "rain") status = "слушает дождь"
        if (mood == "idle" && wk == "snow") status = "смотрит на снег"
        when {
            mood == "fish" && note == "miss" -> text("сорвалась, ничего не поймал", 34, 274, 24, false, STATUS_C)
            mood == "dark" && note == "off" -> text("щёлк! выключил свет", 34, 274, 24, false, STATUS_C)
            else -> text((if (mood == "dark") "" else note) + status, 34, 274, 24, false, STATUS_C)
        }

        // счётчик пойманных рыб
        if (fishCount > 0) {
            fish(590.0, 289.0, 0.5)
            text("×$fishCount", 606, 276, 20, false, STATUS_C)
        }

        return bmp
    }
}
