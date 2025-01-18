package com.example.adaptanklebrace

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.widget.CheckBox
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import com.example.adaptanklebrace.adapters.RecoveryPlanOverviewTableRowAdapter
import com.example.adaptanklebrace.data.Exercise
import com.example.adaptanklebrace.enums.CalendarDay
import com.example.adaptanklebrace.utils.Converters
import com.example.adaptanklebrace.utils.ExerciseUtil
import com.example.adaptanklebrace.utils.SharedPreferencesUtil
import java.util.Calendar

class NotificationsActivity : AppCompatActivity() {

    private lateinit var weeklyDateInput: EditText
    private lateinit var weeklyTimeInput: EditText
    private lateinit var dailyTimeInput: EditText
    private lateinit var weeklyNotificationCheckbox: CheckBox
    private lateinit var dailyNotificationCheckbox: CheckBox

    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var sharedPreferencesSettings: SharedPreferences
    private var recoveryPlanActivity = RecoveryPlanActivity()
    private var mainActivity = MainActivity()

    private lateinit var exerciseAdapter: RecoveryPlanOverviewTableRowAdapter
    private var exercises: MutableList<Exercise> = mutableListOf()

    private val WEEKLY_NOTIFICATION_REQUEST_CODE = 100
    private val DAILY_NOTIFICATION_REQUEST_CODE = 200

    companion object {
        const val NOTIFICATIONS_PREFERENCE = "AppNotifications"
        const val WEEKLY_NOTIFICATION_KEY= "weeklyNotificationReminder"
        const val DAILY_NOTIFICATION_KEY= "dailyNotificationReminder"
        const val WEEKLY_DATE_KEY= "weeklyDateInput"
        const val WEEKLY_TIME_KEY= "weeklyTimeInput"
        const val DAILY_TIME_KEY= "dailyTimeInput"

        var notificationsEnabled: Boolean = false
        var weeklyNotificationEnabled: Boolean = false
        var dailyNotificationEnabled: Boolean = false
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_notifications)

        // Initialize SharedPreferences
        sharedPreferences = getSharedPreferences(NOTIFICATIONS_PREFERENCE, MODE_PRIVATE)

        // Set up the toolbar
        val toolbar: Toolbar = findViewById(R.id.notificationsToolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.title = getString(R.string.notifications)

        // Enable the back button
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)

        // Handle the back button click
        toolbar.setNavigationOnClickListener {
            @Suppress("DEPRECATION")
            onBackPressed() // Go back to the previous activity
        }

        // Initialize views
        weeklyDateInput = findViewById(R.id.weeklyDateInput)
        weeklyTimeInput = findViewById(R.id.weeklyTimeInput)
        dailyTimeInput = findViewById(R.id.dailyTimeInput)
        weeklyNotificationCheckbox = findViewById(R.id.weeklyNotificationPermissionCheckbox)
        dailyNotificationCheckbox = findViewById(R.id.dailyNotificationPermissionCheckbox)

        // Load and set saved preferences
        weeklyNotificationCheckbox.isChecked = SharedPreferencesUtil.getPreference(sharedPreferences, WEEKLY_NOTIFICATION_KEY, false)
        dailyNotificationCheckbox.isChecked = SharedPreferencesUtil.getPreference(sharedPreferences, DAILY_NOTIFICATION_KEY, false)
        weeklyDateInput.setText(SharedPreferencesUtil.getPreference(sharedPreferences, WEEKLY_DATE_KEY, ""))
        weeklyTimeInput.setText(SharedPreferencesUtil.getPreference(sharedPreferences, WEEKLY_TIME_KEY, ""))
        dailyTimeInput.setText(SharedPreferencesUtil.getPreference(sharedPreferences, DAILY_TIME_KEY, ""))

        // Setup the adapter
        exerciseAdapter = RecoveryPlanOverviewTableRowAdapter(exercises, mainActivity)

        // Load data for current week on activity start
        val currentWeek = recoveryPlanActivity.calculateWeekRange(Calendar.getInstance())
        recoveryPlanActivity.loadExerciseWeekData(this, exerciseAdapter, currentWeek)

        // Set click listeners for the inputs
        weeklyDateInput.setOnClickListener {
            ExerciseUtil.showDayPickerDialog(this, weeklyDateInput) { selectedDay ->
                SharedPreferencesUtil.savePreference(sharedPreferences, WEEKLY_DATE_KEY, selectedDay)
                if (weeklyNotificationCheckbox.isChecked && !completedWeeklyProgress()) {
                    scheduleWeeklyNotification()
                }
            }
        }
        weeklyTimeInput.setOnClickListener {
            ExerciseUtil.showTimePickerDialog(this, weeklyTimeInput) { selectedTime ->
                // Save the selected time to app storage
                val time = Converters.convertLocalTimeToString(selectedTime)
                SharedPreferencesUtil.savePreference(sharedPreferences, WEEKLY_TIME_KEY, time)
                if (weeklyNotificationCheckbox.isChecked && !completedWeeklyProgress()) {
                    scheduleWeeklyNotification()
                }
            }
        }
        dailyTimeInput.setOnClickListener {
            ExerciseUtil.showTimePickerDialog(this, dailyTimeInput) { selectedTime ->
                // Save the selected time to app storage
                val time = Converters.convertLocalTimeToString(selectedTime)
                SharedPreferencesUtil.savePreference(sharedPreferences, DAILY_TIME_KEY, time)
                if (dailyNotificationCheckbox.isChecked) {
                    scheduleDailyNotification()
                }
            }
        }

        weeklyNotificationCheckbox.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                if (weeklyDateInput.text.toString().isNotEmpty()) {
                    if (weeklyTimeInput.text.toString().isNotEmpty()) {
                        SharedPreferencesUtil.savePreference(
                            sharedPreferences,
                            WEEKLY_NOTIFICATION_KEY,
                            true
                        )
                        if (notificationsEnabled) {
                            weeklyNotificationEnabled = true
                            if (!completedWeeklyProgress()) {
                                scheduleWeeklyNotification()
                            }
                        } else {
                            ExerciseUtil.showToast(
                                this,
                                layoutInflater,
                                "Please enable notification permissions in the settings."
                            )
                            weeklyNotificationCheckbox.isChecked = false
                        }
                    } else {
                        ExerciseUtil.showToast(
                            this,
                            layoutInflater,
                            "Please select a time before activating the notification."
                        )
                        weeklyNotificationCheckbox.isChecked = false
                    }
                } else {
                    ExerciseUtil.showToast(
                        this,
                        layoutInflater,
                        "Please select a day before activating the notification."
                    )
                    weeklyNotificationCheckbox.isChecked = false
                }
            } else {
                // Cancel notification when checkbox is no longer checked
                cancelWeeklyNotification()
            }
        }
        dailyNotificationCheckbox.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                if (dailyTimeInput.text.toString().isNotEmpty()) {
                    SharedPreferencesUtil.savePreference(
                        sharedPreferences,
                        DAILY_NOTIFICATION_KEY,
                        true
                    )
                    if (notificationsEnabled) {
                        dailyNotificationEnabled = true
                        scheduleDailyNotification()
                    } else {
                        ExerciseUtil.showToast(
                            this,
                            layoutInflater,
                            "Please enable notification permissions in the settings."
                        )
                        dailyNotificationCheckbox.isChecked = false
                    }
                } else {
                    ExerciseUtil.showToast(
                        this,
                        layoutInflater,
                        "Please select a time before activating the notification."
                    )
                    dailyNotificationCheckbox.isChecked = false
                }
            } else {
                // Cancel notification when checkbox is no longer checked
                cancelDailyNotification()
            }
        }
    }

    override fun onResume() {
        super.onResume()

        // Get permissions from SettingsActivity
        sharedPreferencesSettings = getSharedPreferences(SettingsActivity.SETTINGS_PREFERENCE, MODE_PRIVATE)
        notificationsEnabled = SharedPreferencesUtil.getPreference(sharedPreferencesSettings, SettingsActivity.NOTIFICATIONS_PERMISSION_KEY, false)
    }

    private fun scheduleWeeklyNotification() {
        cancelWeeklyNotification()

        val day = weeklyDateInput.text.toString()
        val time = weeklyTimeInput.text.toString()
        val dayOfWeek = CalendarDay.convertDayToCalendar(day)
        val timeParts = time.split(":").map { it.toInt() }

        val calendar = Calendar.getInstance().apply {
            set(Calendar.DAY_OF_WEEK, dayOfWeek)
            set(Calendar.HOUR_OF_DAY, timeParts[0])
            set(Calendar.MINUTE, timeParts[1])
            set(Calendar.SECOND, 0)
        }

        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(this, NotificationReceiver::class.java).apply {
            putExtra("title", "Weekly Reminder")
            putExtra("message", "It's time to complete your weekly exercise goals!")
        }
        val pendingIntent = PendingIntent.getBroadcast(this, WEEKLY_NOTIFICATION_REQUEST_CODE, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

        alarmManager.setRepeating(
            AlarmManager.RTC_WAKEUP,
            calendar.timeInMillis,
            AlarmManager.INTERVAL_DAY * 7,
            pendingIntent
        )
    }

    private fun scheduleDailyNotification() {
        cancelDailyNotification()

        val time = dailyTimeInput.text.toString()
        val timeParts = time.split(":").map { it.toInt() }

        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, timeParts[0])
            set(Calendar.MINUTE, timeParts[1])
            set(Calendar.SECOND, 0)
        }

        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(this, NotificationReceiver::class.java).apply {
            putExtra("title", "Daily Reminder")
            putExtra("message", "Don't forget to complete your exercises today!")
        }
        val pendingIntent = PendingIntent.getBroadcast(this, DAILY_NOTIFICATION_REQUEST_CODE, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

        alarmManager.setRepeating(
            AlarmManager.RTC_WAKEUP,
            calendar.timeInMillis,
            AlarmManager.INTERVAL_DAY,
            pendingIntent
        )
    }

    private fun cancelWeeklyNotification() {
        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(this, NotificationReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(this, WEEKLY_NOTIFICATION_REQUEST_CODE, intent, PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE)

        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
        }
    }

    private fun cancelDailyNotification() {
        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(this, NotificationReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(this, DAILY_NOTIFICATION_REQUEST_CODE, intent, PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE)

        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
        }
    }

    private fun completedWeeklyProgress(): Boolean {
        val currentWeek = recoveryPlanActivity.calculateWeekRange(Calendar.getInstance())
        val exerciseGoalsForCurrentWeek = exerciseAdapter.getExercises()
        val weeklyProgress = recoveryPlanActivity.calculateWeeklyProgress(this, exerciseAdapter, exerciseGoalsForCurrentWeek, currentWeek)
        return (weeklyProgress >= 100.0)
    }
}
