package com.patsyk.widget

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast

/**
 * Открывается только из меню приложений: спрашивает геолокацию (для погоды) и закрывается.
 * Сам Пацык живёт на виджете полностью самостоятельно.
 */
class MenuActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION), 1)
        } else {
            updateLocation()
            done()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) updateLocation()
        done()
    }

    private fun done() {
        Toast.makeText(this, "Пацык живёт сам — добавь виджет «Дом Пацыка» на рабочий стол", Toast.LENGTH_LONG).show()
        val app = applicationContext
        Thread { PatsykWidget.refresh(app, true) }.start()
        finish()
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
        if (!state.hasLoc || Math.abs(state.lat - loc.latitude) > 0.03 || Math.abs(state.lon - loc.longitude) > 0.03) {
            state.setLoc(loc.latitude, loc.longitude)
        }
    }
}
