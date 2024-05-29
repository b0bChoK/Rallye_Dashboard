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

//        requestPermissions(
//            arrayOf<String>(
//                Manifest.permission.,
//                Manifest.permission.MANAGE_EXTERNAL_STORAGE
//            ),
//            1
//        )
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

    override fun onKeyUp(keyCode: Int, event: KeyEvent?): Boolean {
        Log.d(TAG, "Press Key Up %d".format(keyCode))
        var res = false
        for (f in supportFragmentManager.fragments) {
            if (f is DashboardFragment)
                res = f.onKeyUp(keyCode)
        }

        return if (res)
            true
        else
            super.onKeyUp(keyCode, event)
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        Log.d(TAG, "Press Key Down %d".format(keyCode))
        var res = false
        for (f in supportFragmentManager.fragments) {
            if (f is DashboardFragment)
                res = f.onKeyDown(keyCode)
        }

        return if (res)
            true
        else
            super.onKeyDown(keyCode, event)
    }

    //https://medium.com/jodel-engineering/android-volume-keys-as-camera-trigger-with-kotlin-e75d20838706
//    override fun dispatchKeyEvent(event: KeyEvent?): Boolean {
//        val keyCode = event?.keyCode
//        val action = event?.action
//
//
//        if (action == KeyEvent.ACTION_DOWN)
//
//        return super.dispatchKeyEvent(event)
//    }

    override fun onKeyLongPress(keyCode: Int, event: KeyEvent?): Boolean {
        Log.d(TAG, "Long Press Key %d".format(keyCode))

        var res = false

        for (f in supportFragmentManager.fragments) {
            if (f is DashboardFragment)
                res = f.onKeyLongPress(keyCode)
        }

        return if (res)
            true
        else
            super.onKeyLongPress(keyCode, event)
    }
}