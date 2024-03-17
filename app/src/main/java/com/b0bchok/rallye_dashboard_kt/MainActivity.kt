package com.b0bchok.rallye_dashboard_kt

import android.os.Bundle
import android.view.KeyEvent
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.add
import androidx.fragment.app.commit


class MainActivity : AppCompatActivity() {
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
    }

    override fun onKeyUp(keyCode: Int, event: KeyEvent?): Boolean {
        var res = false
        if (keyCode == KeyEvent.KEYCODE_MENU) {
            for(f in supportFragmentManager.fragments) {
                if (f is DashboardFragment)
                    res = f.onKeyUp(keyCode)
            }
        }

        return if (res)
            true
        else
            super.onKeyUp(keyCode, event)
    }

    override fun onKeyLongPress(keyCode: Int, event: KeyEvent?): Boolean {
        var res = false

        if (keyCode == KeyEvent.KEYCODE_MENU) {
            for(f in supportFragmentManager.fragments) {
                if (f is DashboardFragment)
                    res = f.onKeyLongPress(keyCode)
            }
        }

        return if (res)
            true
        else
            super.onKeyLongPress(keyCode, event)
    }
}