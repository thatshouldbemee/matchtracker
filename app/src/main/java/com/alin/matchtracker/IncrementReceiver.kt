package com.alin.matchtracker

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class IncrementReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == MatchTrackerService.ACTION_INCREMENT) {
            Prefs.incrementMatch(context)
            val updateIntent = Intent(context, MatchTrackerService::class.java).apply {
                action = MatchTrackerService.ACTION_UPDATE
            }
            context.startService(updateIntent)
        }
    }
}
