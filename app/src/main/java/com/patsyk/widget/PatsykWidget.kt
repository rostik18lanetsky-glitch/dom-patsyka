package com.patsyk.widget

import android.app.AlarmManager
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews

/** Виджет на рабочем столе. Обновляется раз в 15 минут и сразу после действий из меню. */
class PatsykWidget : AppWidgetProvider() {

    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        schedule(context)
        refreshAsync(context)
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
        if (intent.action == ACTION_TICK) refreshAsync(context)
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

        fun schedule(context: Context) {
            val am = context.getSystemService(AlarmManager::class.java) ?: return
            am.setInexactRepeating(
                AlarmManager.RTC,
                System.currentTimeMillis() + AlarmManager.INTERVAL_FIFTEEN_MINUTES,
                AlarmManager.INTERVAL_FIFTEEN_MINUTES,
                tickIntent(context)
            )
        }

        /** Перерисовать виджет. fetchWeather=true — можно сходить в интернет (не в главном потоке!) */
        fun refresh(context: Context, fetchWeather: Boolean) {
            val s = State(context)
            val now = System.currentTimeMillis()
            val weather = if (fetchWeather) Weather.get(s, now) else s.weather()
            val (mood, note) = Logic.pickMood(s, now)
            val bmp = Renderer.draw(mood, note, now, weather, s.fedAt)

            val views = RemoteViews(context.packageName, R.layout.widget)
            views.setImageViewBitmap(R.id.img, bmp)
            val open = Intent(context, MenuActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            views.setOnClickPendingIntent(
                R.id.root,
                PendingIntent.getActivity(
                    context, 0, open,
                    PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
                )
            )

            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(ComponentName(context, PatsykWidget::class.java))
            if (ids.isNotEmpty()) manager.updateAppWidget(ids, views)
        }
    }
}
