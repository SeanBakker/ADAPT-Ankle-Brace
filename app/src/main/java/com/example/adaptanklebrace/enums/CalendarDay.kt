package com.example.adaptanklebrace.enums

import java.util.Calendar

enum class CalendarDay(val dayName: String) {
    SUNDAY("Sunday"),
    MONDAY("Monday"),
    TUESDAY("Tuesday"),
    WEDNESDAY("Wednesday"),
    THURSDAY("Thursday"),
    FRIDAY("Friday"),
    SATURDAY("Saturday");

    companion object {
        fun getAllDayNames(): List<String> {
            return values()
                .map { it.dayName }
        }

        fun convertDayToCalendar(day: String): Int {
            return when (day) {
                SUNDAY.dayName -> Calendar.SUNDAY
                MONDAY.dayName -> Calendar.MONDAY
                TUESDAY.dayName -> Calendar.TUESDAY
                WEDNESDAY.dayName -> Calendar.WEDNESDAY
                THURSDAY.dayName -> Calendar.THURSDAY
                FRIDAY.dayName -> Calendar.FRIDAY
                SATURDAY.dayName -> Calendar.SATURDAY
                else -> Calendar.SATURDAY
            }
        }
    }
}