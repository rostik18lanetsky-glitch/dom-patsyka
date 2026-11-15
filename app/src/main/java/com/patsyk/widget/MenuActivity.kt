package com.patsyk.widget

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.Build
import android.os.Bundle

/**
 * Меню по нажатию на виджет: покормить, погладить, рыбалка, спать, свет.
 * Здесь же спрашиваем доступ к геолокации и запоминаем координаты для погоды.
 */
class MenuActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION), 1)
        } else {
            updateLocation()
            showMenu()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) updateLocation()
        showMenu()
    }

    @SuppressLint("MissingPermission")
    private fun updateLocation() {
        val lm = getSystemService(LocationManager::class.java) ?: return
        val state = State(this)
        try {
            var best: Location? = null
            for (provider in lm.getProviders(true)) {
                val loc = lm.getLastKnownLocation(provider) ?: continue
                if (best == null || loc.time > best.time) best = loc
            }
            best?.let { saveLoc(state, it) }

            // свежая точка (Android 11+), придёт чуть позже
            if (Build.VERSION.SDK_INT >= 30 && lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                val app = applicationContext
                lm.getCurrentLocation(LocationManager.NETWORK_PROVIDER, null, mainExecutor) { loc ->
                    if (loc != null) {
                        saveLoc(State(app), loc)
                        Thread { PatsykWidget.refresh(app, true) }.start()
                    }
                }
            }
        } catch (e: Exception) {
            // нет геолокации — погода для Киева или последней точки
        }
    }

    private fun saveLoc(state: State, loc: Location) {
        // меняем координаты только если сдвинулись заметно (~3 км), чтобы не сбрасывать кэш погоды
        if (!state.hasLoc || Math.abs(state.lat - loc.latitude) > 0.03 || Math.abs(state.lon - loc.longitude) > 0.03) {
            state.setLoc(loc.latitude, loc.longitude)
        }
    }

    private fun showMenu() {
        val s = State(this)
        val now = System.currentTimeMillis()
        val darkNow = Logic.pickMood(s, now).mood == "dark"
        val items = arrayOf(
            "🧀 Покормить",
            "🤚 Погладить",
            "🎣 Отправить на рыбалку",
            "🌙 Уложить спать",
            if (darkNow) "💡 Включить свет" else "💡 Выключить свет",
        )
        val acts = arrayOf("eat", "happy", "fish", "sleep")

        AlertDialog.Builder(this, android.R.style.Theme_DeviceDefault_Dialog_Alert)
            .setTitle(if (s.fishCount > 0) "Пацык · поймано рыб: ${s.fishCount}" else "Пацык")
            .setItems(items) { _, i ->
                if (i < acts.size && acts[i] == "fish") {
                    s.fishUntil = now + 5 * 60_000L   // рыбачит 5 минут — жми на прорубь!
                    s.action = null
                } else if (i < acts.size) {
                    s.action = acts[i]
                    s.actionAt = now
                    s.fishUntil = 0L
                    if (acts[i] == "eat") s.fedAt = now
                } else {
                    s.lightOff = !darkNow
                    s.lightAt = now
                    s.action = null
                }
                val app = applicationContext
                Thread { PatsykWidget.refresh(app, true) }.start()
            }
            .setNegativeButton("Закрыть", null)
            .setOnDismissListener { finish() }
            .show()

        // заодно обновим виджет (вдруг погода/координаты поменялись)
        val app = applicationContext
        Thread { PatsykWidget.refresh(app, true) }.start()
    }
}
