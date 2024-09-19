package io.opentelemetry.android.demo.about

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.bottomnavigation.BottomNavigationView
import io.opentelemetry.android.demo.R

class AboutActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_about)

        val bottomNavigationView: BottomNavigationView = findViewById(R.id.bottom_navigation)

        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, AppFeaturesFragment())
                .commit()
            bottomNavigationView.selectedItemId = R.id.navigation_app_features
        }

        bottomNavigationView.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.navigation_exit -> {
                    finish()
                    true
                }
                R.id.navigation_app_features -> {
                    supportFragmentManager.beginTransaction()
                        .replace(R.id.fragment_container, AppFeaturesFragment())
                        .commit()
                    true
                }
                R.id.navigation_about_opentelemetry -> {
                    supportFragmentManager.beginTransaction()
                        .replace(R.id.fragment_container, AboutOpenTelemetryFragment())
                        .commit()
                    true
                }
                else -> false
            }
        }
    }
}
