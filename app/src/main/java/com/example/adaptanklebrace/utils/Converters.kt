package com.example.adaptanklebrace.utils

import java.text.SimpleDateFormat
import java.time.LocalTime
import java.util.Date
import java.util.Locale

class Converters {
    companion object {
        fun convertDateToString(date: Date): String {
            val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
            return timeFormat.format(date)
        }

        fun convertLocalTimeToString(localTime: LocalTime): String {
            return localTime.format(ExerciseUtil.timeFormatter)
        }
    }
}
