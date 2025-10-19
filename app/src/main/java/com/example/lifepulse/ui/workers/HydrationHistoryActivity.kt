package com.example.lifepulse.ui.workers

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.lifepulse.R

class HydrationHistoryActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // reuse a simple container layout, or create one named activity_container.xml
        setContentView(R.layout.activity_container)

        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.container, HydrationHistoryFragment())
                .commit()
        }
    }
}
