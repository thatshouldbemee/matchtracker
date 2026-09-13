package com.alin.matchtracker

import android.content.Context
import java.util.concurrent.TimeUnit

/**
 * Simple SharedPreferences wrapper holding season target, season length,
 * total matches played, and the season start timestamp. Reset is manual —
 * only [resetSeason] clears the counter, nothing resets it automatically.
 */
object Prefs {
    private const val FILE = "match_tracker_prefs"
    private const val KEY_TARGET = "target_matches"
    private const val KEY_SEASON_DAYS = "season_days"
    private const val KEY_TOTAL = "total_matches"
    private const val KEY_START = "start_timestamp"

    private fun prefs(context: Context) =
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE)

    fun setup(context: Context, target: Int, seasonDays: Int) {
        prefs(context).edit()
            .putInt(KEY_TARGET, target)
            .putInt(KEY_SEASON_DAYS, seasonDays)
            .putLong(KEY_START, System.currentTimeMillis())
            .putInt(KEY_TOTAL, prefs(context).getInt(KEY_TOTAL, 0))
            .apply()
    }

    fun resetSeason(context: Context) {
        prefs(context).edit()
            .putInt(KEY_TOTAL, 0)
            .putLong(KEY_START, System.currentTimeMillis())
            .apply()
    }

    fun incrementMatch(context: Context): Int {
        val newTotal = getTotal(context) + 1
        prefs(context).edit().putInt(KEY_TOTAL, newTotal).apply()
        return newTotal
    }

    fun getTarget(context: Context) = prefs(context).getInt(KEY_TARGET, 0)
    fun getSeasonDays(context: Context) = prefs(context).getInt(KEY_SEASON_DAYS, 90)
    fun getTotal(context: Context) = prefs(context).getInt(KEY_TOTAL, 0)
    fun getStart(context: Context) = prefs(context).getLong(KEY_START, System.currentTimeMillis())

    /** Whole days elapsed since season start, minimum 0. */
    fun elapsedDays(context: Context): Int {
        val diffMs = System.currentTimeMillis() - getStart(context)
        return TimeUnit.MILLISECONDS.toDays(diffMs).toInt().coerceAtLeast(0)
    }

    fun remainingDays(context: Context): Int =
        (getSeasonDays(context) - elapsedDays(context)).coerceAtLeast(1)

    fun remainingTarget(context: Context): Int =
        (getTarget(context) - getTotal(context)).coerceAtLeast(0)

    /** Matches/day required from now on to still hit the season target. */
    fun requiredPace(context: Context): Double =
        remainingTarget(context).toDouble() / remainingDays(context).toDouble()

    /** Average matches/day actually played so far this season. */
    fun currentPace(context: Context): Double {
        val days = elapsedDays(context).coerceAtLeast(1)
        return getTotal(context).toDouble() / days.toDouble()
    }

    fun summaryText(context: Context): String {
        val total = getTotal(context)
        val target = getTarget(context)
        val pace = String.format("%.1f", requiredPace(context))
        val avg = String.format("%.1f", currentPace(context))
        return "Total: $total/$target  |  Perlu: $pace/hari  |  Rata-rata: $avg/hari"
    }
}
