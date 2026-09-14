package com.alin.matchtracker

import android.content.Context
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlin.math.ceil

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
    private const val KEY_DAILY_COUNT = "daily_count"
    private const val KEY_DAILY_DATE = "daily_date"

    private val dayFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    private fun todayKey() = dayFormat.format(Date())

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
            .putInt(KEY_DAILY_COUNT, 0)
            .putString(KEY_DAILY_DATE, todayKey())
            .commit()
    }

    fun incrementMatch(context: Context): Int {
        val newTotal = getTotal(context) + 1
        val newDaily = getDailyCount(context) + 1
        prefs(context).edit()
            .putInt(KEY_TOTAL, newTotal)
            .putInt(KEY_DAILY_COUNT, newDaily)
            .putString(KEY_DAILY_DATE, todayKey())
            .apply()
        return newTotal
    }

    /** Matches logged today. Auto-rolls over to 0 when the calendar day changes — no manual action needed. */
    fun getDailyCount(context: Context): Int {
        val storedDate = prefs(context).getString(KEY_DAILY_DATE, null)
        return if (storedDate == todayKey()) prefs(context).getInt(KEY_DAILY_COUNT, 0) else 0
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

    /** Target match hari ini, dibulatkan ke atas supaya tetap aman kejar target season. */
    fun dailyTarget(context: Context): Int = ceil(requiredPace(context)).toInt().coerceAtLeast(0)

    fun titleText(context: Context): String {
        val daily = getDailyCount(context)
        val target = dailyTarget(context)
        return if (daily >= target && target > 0) {
            "Hari ini: $daily/$target ✅ target tercapai"
        } else {
            "Hari ini: $daily/$target match"
        }
    }

    fun summaryText(context: Context): String {
        val total = getTotal(context)
        val target = getTarget(context)
        val daily = getDailyCount(context)
        val dailyGoal = dailyTarget(context)
        val remaining = remainingDays(context)

        val statusLine = when {
            dailyGoal <= 0 -> "Target season sudah tercapai 🎉"
            daily > dailyGoal -> "Lebih ${daily - dailyGoal} match dari target hari ini, kerja bagus!"
            daily == dailyGoal -> "Pas target hari ini, lanjutkan besok!"
            else -> "Kurang ${dailyGoal - daily} match lagi hari ini"
        }

        return "Hari ini: $daily/$dailyGoal match\n" +
            "Season: $total/$target match  •  sisa $remaining hari\n" +
            statusLine
    }
}
