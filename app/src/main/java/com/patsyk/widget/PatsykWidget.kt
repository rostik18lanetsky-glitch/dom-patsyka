package com.patsyk.widget

import android.app.AlarmManager
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.widget.RemoteViews

/**
 * Виджет на рабочем столе. Пацык живёт сам: виджет перерисовывается
 * в момент, когда заканчивается очередной шаг (обычно каждые 15 секунд).
 */
class PatsykWidget : AppWidgetProvider() {

    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        refreshAsync(context)
    }

    override fun onEnabled(context: Context) {
        refreshAsync(context)
    }

    override fun onDisabled(context: Context) {
        val am = context.getSystemService(AlarmManager::class.java) ?: return
        am.cancel(tickIntent(context))
        am.cancel(safetyIntent(context))
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        when (intent.action) {
            ACTION_TICK, Intent.ACTION_BOOT_COMPLETED, Intent.ACTION_MY_PACKAGE_REPLACED -> refreshAsync(context)
        }
    }

    private fun refreshAsync(context: Context) {
        val pending = goAsync()
        val app = context.applicationContext
        Thread {
            try {
                refresh(app, true)
            } catch (e: Exception) {
                // не роняем виджет
            } finally {
                pending.finish()
            }
        }.start()
    }

    companion object {
        const val ACTION_TICK = "com.patsyk.widget.TICK"

        private fun tickIntent(context: Context): PendingIntent =
            PendingIntent.getBroadcast(
                context, 1,
                Intent(context, PatsykWidget::class.java).setAction(ACTION_TICK),
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )

        private fun safetyIntent(context: Context): PendingIntent =
            PendingIntent.getBroadcast(
                context, 3,
                Intent(context, PatsykWidget::class.java).setAction(ACTION_TICK),
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )

        /**
         * Следующее обновление — точно к концу текущего шага.
         * RTC без пробуждения: при выключенном экране ждёт и батарею не тратит.
         */
        private fun scheduleNext(context: Context, at: Long) {
            val am = context.getSystemService(AlarmManager::class.java) ?: return
            val t = maxOf(at, System.currentTimeMillis() + 3_000L)
            val pi = tickIntent(context)
            try {
                if (Build.VERSION.SDK_INT >= 31 && !am.canScheduleExactAlarms()) am.set(AlarmManager.RTC, t, pi)
                else am.setExact(AlarmManager.RTC, t, pi)
            } catch (e: SecurityException) {
                am.set(AlarmManager.RTC, t, pi)
            }
            // страховка: если цепочка вдруг оборвётся — раз в 15 минут перезапускаем
            am.setInexactRepeating(
                AlarmManager.RTC,
                System.currentTimeMillis() + AlarmManager.INTERVAL_FIFTEEN_MINUTES,
                AlarmManager.INTERVAL_FIFTEEN_MINUTES,
                safetyIntent(context)
            )
        }

        /** Перерисовать виджет (не в главном потоке — может сходить за погодой) */
        @Synchronized
        fun refresh(context: Context, fetchWeather: Boolean) {
            val s = State(context)
            val now = System.currentTimeMillis()
            val weather = if (fetchWeather) Weather.get(s, now) else s.weather()
            val (m, stepEnd) = Logic.current(s, now)

            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(ComponentName(context, PatsykWidget::class.java))
            if (ids.isEmpty()) return
            scheduleNext(context, stepEnd)

            // Полная анимация; если телефону не хватит памяти на кадры — меньше кадров.
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
