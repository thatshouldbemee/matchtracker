package com.alin.matchtracker

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import android.widget.Button
import android.widget.EditText
import android.widget.TextView

class MainActivity : AppCompatActivity() {

    private lateinit var targetInput: EditText
    private lateinit var seasonDaysInput: EditText
    private lateinit var summaryText: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        targetInput = findViewById(R.id.targetInput)
        seasonDaysInput = findViewById(R.id.seasonDaysInput)
        summaryText = findViewById(R.id.summaryText)

        targetInput.setText(Prefs.getTarget(this).takeIf { it > 0 }?.toString() ?: "")
        seasonDaysInput.setText(Prefs.getSeasonDays(this).toString())

        findViewById<Button>(R.id.startButton).setOnClickListener {
            val target = targetInput.text.toString().toIntOrNull() ?: 0
            val seasonDays = seasonDaysInput.text.toString().toIntOrNull() ?: 90
            Prefs.setup(this, target, seasonDays)
            refreshSummary()
            if (hasNotificationPermission()) {
                startTrackerService()
            } else {
                requestNotificationPermissionIfNeeded()
            }
        }

        findViewById<Button>(R.id.resetButton).setOnClickListener {
            Prefs.resetSeason(this)
            refreshSummary()
            startTrackerService()
        }

        refreshSummary()
    }

    override fun onResume() {
        super.onResume()
        refreshSummary()
    }

    private fun refreshSummary() {
        summaryText.text = Prefs.summaryText(this)
    }

    private fun startTrackerService() {
        val intent = Intent(this, MatchTrackerService::class.java)
        ContextCompat.startForegroundService(this, intent)
    }

    private fun hasNotificationPermission(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return true
        return ContextCompat.checkSelfPermission(
            this, Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ActivityCompat.requestPermissions(
                this, arrayOf(Manifest.permission.POST_NOTIFICATIONS), 100
            )
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 100 && grantResults.isNotEmpty() &&
            grantResults[0] == PackageManager.PERMISSION_GRANTED
        ) {
            startTrackerService()
        }
    }
}
