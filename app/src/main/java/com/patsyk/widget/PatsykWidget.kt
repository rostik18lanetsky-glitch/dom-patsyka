package com.patsyk.widget

import android.app.AlarmManager
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews

/** Виджет на рабочем столе. Обновляется раз в минуту и сразу после нажатий. */
class PatsykWidget : AppWidgetProvider() {

    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        schedule(context)
        refreshAsync(context, true)
    }

    override fun onEnabled(context: Context) {
        schedule(context)
    }

    override fun onDisabled(context: Context) {
        val am = context.getSystemService(AlarmManager::class.java)
        am?.cancel(tickIntent(context))
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        when (intent.action) {
            ACTION_TICK -> refreshAsync(context, true)
            ACTION_HOOK -> {
                // нажали на прорубь — пробуем подсечь
                val app = context.applicationContext
                val pending = goAsync()
                Thread {
                    try {
                        Logic.hook(State(app), System.currentTimeMillis())
                        refresh(app, false)
                    } catch (e: Exception) {
                    } finally {
                        pending.finish()
                    }
                }.start()
            }
        }
    }

    private fun refreshAsync(context: Context, fetchWeather: Boolean) {
        val pending = goAsync()
        val app = context.applicationContext
        Thread {
            try {
                refresh(app, fetchWeather)
            } catch (e: Exception) {
                // не роняем виджет
            } finally {
                pending.finish()
            }
        }.start()
    }

    companion object {
        const val ACTION_TICK = "com.patsyk.widget.TICK"
        const val ACTION_HOOK = "com.patsyk.widget.HOOK"

        private fun tickIntent(context: Context): PendingIntent =
            PendingIntent.getBroadcast(
                context, 1,
                Intent(context, PatsykWidget::class.java).setAction(ACTION_TICK),
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )

        private fun hookIntent(context: Context): PendingIntent =
            PendingIntent.getBroadcast(
                context, 2,
                Intent(context, PatsykWidget::class.java).setAction(ACTION_HOOK),
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )

        /** Раз в минуту (RTC без пробуждения: при выключенном экране ждёт, батарею не тратит) */
        fun schedule(context: Context) {
            val am = context.getSystemService(AlarmManager::class.java) ?: return
            am.setRepeating(
                AlarmManager.RTC,
                System.currentTimeMillis() + 60_000L,
                60_000L,
                tickIntent(context)
            )
        }

        /** Перерисовать виджет. fetchWeather=true — можно сходить в интернет (не в главном потоке!) */
        @Synchronized
        fun refresh(context: Context, fetchWeather: Boolean) {
            val s = State(context)
            val now = System.currentTimeMillis()
            val weather = if (fetchWeather) Weather.get(s, now) else s.weather()

            var m = Logic.pickMood(s, now)
            // рыбачит сам — иногда ловит без тебя
            if (m.mood == "fish" && Logic.autoCatch(s, now)) {
                Logic.startCatch(s, now)
                m = Logic.pickMood(s, now)
            }

            val open = Intent(context, MenuActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            val tap = PendingIntent.getActivity(
                context, 0, open,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
            val left = if (m.mood == "fish") hookIntent(context) else tap

            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(ComponentName(context, PatsykWidget::class.java))
            if (ids.isEmpty()) return

            // Пробуем полную анимацию; если телефону не хватит памяти на кадры — меньше кадров.
            for ((count, scale) in QUALITY) {
                try {
                    val views = RemoteViews(context.packageName, R.layout.widget)
                    views.removeAllViews(R.id.flipper)
                    for (f in 0 until count) {
                        val bmp = Renderer.draw(
                            m.mood, m.note, now, weather, s.fedAt, f, count, scale,
                            m.progress, s.fishCount
                        )
                        val fv = RemoteViews(context.packageName, R.layout.frame)
                        fv.setImageViewBitmap(R.id.frame_img, bmp)
                        views.addView(R.id.flipper, fv)
                    }
                    views.setOnClickPendingIntent(R.id.root, tap)
                    views.setOnClickPendingIntent(R.id.zone_right, tap)
                    views.setOnClickPendingIntent(R.id.zone_left, left)
                    manager.updateAppWidget(ids, views)
                    return
                } catch (e: Exception) {
                    // слишком много памяти — пробуем вариант попроще
                }
            }
        }

        /** (кол-во кадров, масштаб): от красивого к простому */
        private val QUALITY = listOf(10 to 1.2f, 8 to 1.0f, 4 to 1.0f, 1 to 2f)
    }
}
