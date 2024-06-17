package com.b0bchok.rallye_dashboard_kt

import android.os.Bundle
import android.util.Log
import android.view.KeyEvent
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.app.ActivityCompat
import androidx.fragment.app.add
import androidx.fragment.app.commit
import androidx.preference.PreferenceManager


class MainActivity : AppCompatActivity() {
    companion object {
        private const val TAG = "MainActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        setContentView(R.layout.activity_main)

        if (savedInstanceState == null) {
            supportFragmentManager.commit {
                setReorderingAllowed(true)
                add<DashboardFragment>(R.id.fragment_container)
            }
        }

        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)
        if (sharedPreferences != null) {
            when (sharedPreferences.getString("THEME_SELECTION", "Auto")) {
                "Light" -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
                "Dark" -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
                else -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
            }
        }
    }

    override fun dispatchKeyEvent(event: KeyEvent?): Boolean {
        val keyCode = event?.keyCode
        val action = event?.action
        var res = false

        Log.d(TAG, "DispatchKeyEvent %s keyCode %d, action %d".format(event.toString(), keyCode, action))

        for (f in supportFragmentManager.fragments) {
            if (f is DashboardFragment) {
                Log.d(TAG, "Send it to DashboardFragment")
                res = f.dispatchKeyEvent(event)
            }

            if (f is RemoteFragment) {
                Log.d(TAG, "Send it to RemoteFragment")
                res = f.dispatchKeyEvent(event)
            }

        }

        if (res)
            return res
        else
            return super.dispatchKeyEvent(event)
    }
}