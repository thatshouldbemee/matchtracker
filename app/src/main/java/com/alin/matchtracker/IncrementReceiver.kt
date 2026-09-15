package com.alin.matchtracker

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class IncrementReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            MatchTrackerService.ACTION_WIN -> Prefs.incrementMatch(context, isWin = true)
            MatchTrackerService.ACTION_LOSE -> Prefs.incrementMatch(context, isWin = false)
            else -> return
        }
        val updateIntent = Intent(context, MatchTrackerService::class.java).apply {
            action = MatchTrackerService.ACTION_UPDATE
        }
        context.startService(updateIntent)
    }
}
