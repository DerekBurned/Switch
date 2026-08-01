package com.example.messenger.util

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

object DateUtils {

    private val timeFormat: ThreadLocal<SimpleDateFormat> = ThreadLocal.withInitial {
        SimpleDateFormat("HH:mm", Locale.getDefault())
    }
    private val dateFormat: ThreadLocal<SimpleDateFormat> = ThreadLocal.withInitial {
        SimpleDateFormat("MM/dd 'at' HH:mm", Locale.getDefault())
    }
    private val dayMonthFormat: ThreadLocal<SimpleDateFormat> = ThreadLocal.withInitial {
        SimpleDateFormat("MMMM d", Locale.getDefault())
    }
    private val dayMonthYearFormat: ThreadLocal<SimpleDateFormat> = ThreadLocal.withInitial {
        SimpleDateFormat("MMMM d, yyyy", Locale.getDefault())
    }

    private fun time(): SimpleDateFormat = timeFormat.get()!!
    private fun date(): SimpleDateFormat = dateFormat.get()!!

    fun formatLastSeen(timestamp: Long): String {
        if (timestamp <= 0L) return "Offline"

        val now = System.currentTimeMillis()
        val diff = now - timestamp

        val minutes = TimeUnit.MILLISECONDS.toMinutes(diff)
        val hours = TimeUnit.MILLISECONDS.toHours(diff)

        return when {
            minutes < 1 -> "Last seen just now"
            minutes in 1..59 -> "Last seen $minutes min ago"
            hours in 1..23 -> "Last seen $hours hr ago"
            hours in 24..48 -> "Last seen yesterday at ${time().format(Date(timestamp))}"
            else -> "Last seen ${date().format(Date(timestamp))}"
        }
    }

    fun formatMessageTime(timestamp: Long): String {
        if (timestamp <= 0L) return ""
        return time().format(Date(timestamp))
    }

    fun startOfDay(timestamp: Long): Long {
        val cal = Calendar.getInstance().apply {
            timeInMillis = timestamp
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return cal.timeInMillis
    }

    fun formatDayDivider(timestamp: Long): String {
        if (timestamp <= 0L) return ""
        val message = Calendar.getInstance().apply { timeInMillis = timestamp }
        val today = Calendar.getInstance()
        val yesterday = (today.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, -1) }
        fun sameDay(a: Calendar, b: Calendar): Boolean =
            a.get(Calendar.YEAR) == b.get(Calendar.YEAR) &&
                a.get(Calendar.DAY_OF_YEAR) == b.get(Calendar.DAY_OF_YEAR)
        return when {
            sameDay(message, today) -> "Today"
            sameDay(message, yesterday) -> "Yesterday"
            message.get(Calendar.YEAR) == today.get(Calendar.YEAR) ->
                dayMonthFormat.get()!!.format(Date(timestamp))
            else -> dayMonthYearFormat.get()!!.format(Date(timestamp))
        }
    }

    fun formatDuration(seconds: Int): String {
        if (seconds <= 0) return "0:00"
        val hours = seconds / 3600
        val minutes = (seconds % 3600) / 60
        val secs = seconds % 60
        return if (hours > 0) {
            "%d:%02d:%02d".format(hours, minutes, secs)
        } else {
            "%d:%02d".format(minutes, secs)
        }
    }
}
