package com.example.adaptanklebrace

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.CheckBox
import android.widget.EditText
import com.example.adaptanklebrace.adapters.RecoveryPlanOverviewExerciseTableRowAdapter
import com.example.adaptanklebrace.adapters.RecoveryPlanOverviewMetricTableRowAdapter
import com.example.adaptanklebrace.data.Exercise
import com.example.adaptanklebrace.data.Metric
import com.example.adaptanklebrace.enums.CalendarDay
import com.example.adaptanklebrace.utils.Converters
import com.example.adaptanklebrace.utils.GeneralUtil
import com.example.adaptanklebrace.utils.SharedPreferencesUtil
import java.util.Calendar

class NotificationsActivity : BaseActivity() {

    private lateinit var weeklyDateInput: AutoCompleteTextView
    private lateinit var weeklyTimeInput: EditText
    private lateinit var dailyTimeInput: EditText
    private lateinit var weeklyNotificationCheckbox: CheckBox
    private lateinit var dailyNotificationCheckbox: CheckBox

    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var sharedPreferencesSettings: SharedPreferences
    private var recoveryPlanActivity = RecoveryPlanActivity()
    private var mainActivity = MainActivity()

    private lateinit var exerciseAdapter: RecoveryPlanOverviewExerciseTableRowAdapter
    private var exercises: MutableList<Exercise> = mutableListOf()

    private lateinit var metricAdapter: RecoveryPlanOverviewMetricTableRowAdapter
    private var metrics: MutableList<Metric> = mutableListOf()

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
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_notifications)

        // Initialize SharedPreferences
        sharedPreferences = getSharedPreferences(NOTIFICATIONS_PREFERENCE, MODE_PRIVATE)

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

        // Setup the adapters
        exerciseAdapter = RecoveryPlanOverviewExerciseTableRowAdapter(exercises, mainActivity)
        metricAdapter = RecoveryPlanOverviewMetricTableRowAdapter(metrics, mainActivity)

        // Load data for current week on activity start
        val currentWeek = recoveryPlanActivity.calculateWeekRange(Calendar.getInstance())
        recoveryPlanActivity.loadExerciseWeekData(this, exerciseAdapter, currentWeek)
        recoveryPlanActivity.loadMetricWeekData(this, metricAdapter, currentWeek)

        // Initialize weeklyDateInput dropdown
        val weekDays = CalendarDay.getAllDayNames()
        val adapter = ArrayAdapter(this, R.layout.dropdown_items, weekDays)
        weeklyDateInput.setAdapter(adapter)

        // Set click listeners for the inputs
        weeklyDateInput.setOnClickListener {
            // Show all suggestions when the field gains focus
            weeklyDateInput.setDropDownBackgroundResource(R.color.blue_2)
            weeklyDateInput.showDropDown()
        }
        weeklyDateInput.setOnItemClickListener { _, _, position, _ ->
            // Handle selection from the dropdown
            val selectedDay = weeklyDateInput.adapter.getItem(position) as String
            SharedPreferencesUtil.savePreference(sharedPreferences, WEEKLY_DATE_KEY, selectedDay)

            // Schedule the notification based on logic
            if (weeklyNotificationCheckbox.isChecked && !completedWeeklyProgress()) {
                scheduleWeeklyNotification()
            }
        }
        weeklyTimeInput.setOnClickListener {
            GeneralUtil.showTimePickerDialog(this, weeklyTimeInput) { selectedTime ->
                // Save the selected time to app storage
                val time = Converters.convertLocalTimeToString(selectedTime)
                SharedPreferencesUtil.savePreference(sharedPreferences, WEEKLY_TIME_KEY, time)
                if (weeklyNotificationCheckbox.isChecked && !completedWeeklyProgress()) {
                    scheduleWeeklyNotification()
                }
            }
        }
        dailyTimeInput.setOnClickListener {
            GeneralUtil.showTimePickerDialog(this, dailyTimeInput) { selectedTime ->
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
                        if (notificationsEnabled) {
                            SharedPreferencesUtil.savePreference(
                                sharedPreferences,
                                WEEKLY_NOTIFICATION_KEY,
                                true
                            )
                            if (!completedWeeklyProgress()) {
                                scheduleWeeklyNotification()
                            }
                        } else {
                            GeneralUtil.showToast(
                                this,
                                layoutInflater,
                                getString(R.string.enableNotificationsToast)
                            )
                            weeklyNotificationCheckbox.isChecked = false
                        }
                    } else {
                        GeneralUtil.showToast(
                            this,
                            layoutInflater,
                            getString(R.string.selectNotificationTimeToast)
                        )
                        weeklyNotificationCheckbox.isChecked = false
                    }
                } else {
                    GeneralUtil.showToast(
                        this,
                        layoutInflater,
                        getString(R.string.selectNotificationDayToast)
                    )
                    weeklyNotificationCheckbox.isChecked = false
                }
            } else {
                // Cancel notification when checkbox is no longer checked
                cancelWeeklyNotification()

                SharedPreferencesUtil.savePreference(
                    sharedPreferences,
                    WEEKLY_NOTIFICATION_KEY,
                    false
                )
            }
        }
        dailyNotificationCheckbox.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                if (dailyTimeInput.text.toString().isNotEmpty()) {
                    if (notificationsEnabled) {
                        SharedPreferencesUtil.savePreference(
                            sharedPreferences,
                            DAILY_NOTIFICATION_KEY,
                            true
                        )
                        scheduleDailyNotification()
                    } else {
                        GeneralUtil.showToast(
                            this,
                            layoutInflater,
                            getString(R.string.enableNotificationsToast)
                        )
                        dailyNotificationCheckbox.isChecked = false
                    }
                } else {
                    GeneralUtil.showToast(
                        this,
                        layoutInflater,
                        getString(R.string.selectNotificationTimeToast)
                    )
                    dailyNotificationCheckbox.isChecked = false
                }
            } else {
                // Cancel notification when checkbox is no longer checked
                cancelDailyNotification()

                SharedPreferencesUtil.savePreference(
                    sharedPreferences,
                    DAILY_NOTIFICATION_KEY,
                    false
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()

        // Get permissions from SettingsActivity
        sharedPreferencesSettings = getSharedPreferences(SettingsActivity.SETTINGS_PREFERENCE, MODE_PRIVATE)
        notificationsEnabled = SharedPreferencesUtil.getPreference(sharedPreferencesSettings, SettingsActivity.NOTIFICATIONS_PERMISSION_KEY, false)

        // Update checkboxes
        if (notificationsEnabled) {
            weeklyNotificationCheckbox.isChecked = SharedPreferencesUtil.getPreference(
                sharedPreferences,
                WEEKLY_NOTIFICATION_KEY,
                false
            )
            dailyNotificationCheckbox.isChecked = SharedPreferencesUtil.getPreference(
                sharedPreferences,
                DAILY_NOTIFICATION_KEY,
                false
            )
        } else {
            weeklyNotificationCheckbox.isChecked = false
            dailyNotificationCheckbox.isChecked = false
        }
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
        val metricGoalsForCurrentWeek = metricAdapter.getMetrics()
        val weeklyProgress = recoveryPlanActivity.calculateWeeklyProgress(this, exerciseAdapter, metricAdapter, exerciseGoalsForCurrentWeek, metricGoalsForCurrentWeek, currentWeek)
        return (weeklyProgress >= 100.0)
    }
}
