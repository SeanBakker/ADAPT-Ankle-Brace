package com.example.adaptanklebrace

import android.annotation.SuppressLint
import android.app.DatePickerDialog
import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.os.Bundle
import android.os.Parcelable
import android.view.MotionEvent
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.core.widget.addTextChangedListener
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.adaptanklebrace.RecoveryDataActivity.Companion.RECOVERY_DATA_PREFERENCE
import com.example.adaptanklebrace.adapters.RecoveryExerciseAdapter
import com.example.adaptanklebrace.adapters.RecoveryPlanExerciseTableRowAdapter
import com.example.adaptanklebrace.adapters.RecoveryMetricAdapter
import com.example.adaptanklebrace.adapters.RecoveryPlanMetricTableRowAdapter
import com.example.adaptanklebrace.data.Exercise
import com.example.adaptanklebrace.data.ExerciseInfo
import com.example.adaptanklebrace.data.Metric
import com.example.adaptanklebrace.fragments.AddGoalFreqFragment
import com.example.adaptanklebrace.fragments.AddExerciseGoalRowFragment
import com.example.adaptanklebrace.fragments.AddMetricGoalRowFragment
import com.example.adaptanklebrace.fragments.DeleteRowFragment
import com.example.adaptanklebrace.fragments.StartExerciseWarningFragment
import com.example.adaptanklebrace.utils.ExerciseDataStore
import com.example.adaptanklebrace.utils.ExerciseUtil
import com.example.adaptanklebrace.utils.GeneralUtil
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class RecoveryPlanActivity : AppCompatActivity(), RecoveryPlanExerciseTableRowAdapter.RecoveryPlanCallback,
    RecoveryPlanMetricTableRowAdapter.RecoveryPlanCallback, DeleteRowFragment.OnDeleteListener,
    StartExerciseWarningFragment.OnStartExerciseOrMetricListener {

    private lateinit var dateTextView: TextView
    private lateinit var datePickerButton: Button
    private lateinit var difficultyProgressBar: ProgressBar
    private lateinit var difficultyTextView: TextView
    private lateinit var commentsEditText: EditText
    private lateinit var exportButton: Button
    private lateinit var importButton: Button
    private lateinit var updatePercentagesButton: Button
    private lateinit var deleteRowButton: Button

    // Exercise table variables
    private lateinit var exerciseTableLayout: ConstraintLayout
    private lateinit var exerciseRecyclerView: RecyclerView
    private lateinit var exerciseAdapter: RecoveryPlanExerciseTableRowAdapter
    private var exercises: MutableList<Exercise> = mutableListOf()
    private lateinit var addExerciseButton: Button

    // Metric table variables
    private lateinit var metricTableLayout: ConstraintLayout
    private lateinit var metricRecyclerView: RecyclerView
    private lateinit var metricAdapter: RecoveryPlanMetricTableRowAdapter
    private var metrics: MutableList<Metric> = mutableListOf()
    private lateinit var addMetricButton: Button

    companion object {
        const val RECOVERY_PLAN_PREFERENCE = "RecoveryPlan"
    }

    @SuppressLint("ClickableViewAccessibility", "SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_recovery_plan)

        // Set up the toolbar
        val toolbar: Toolbar = findViewById(R.id.recoveryPlanToolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.title = getString(R.string.recovery_plan)

        // Enable the back button
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)

        // Handle the back button click
        toolbar.setNavigationOnClickListener {
            // Save current state of the data
            saveCurrentDateExerciseData()
            saveCurrentDateMetricData()

            @Suppress("DEPRECATION")
            onBackPressed()
        }

        // Initialize views
        dateTextView = findViewById(R.id.dateText)
        datePickerButton = findViewById(R.id.datePickerButton)
        difficultyProgressBar = findViewById(R.id.difficultyProgressBar)
        difficultyProgressBar.progressTintList = ColorStateList.valueOf(ContextCompat.getColor(this, R.color.red_1))
        difficultyTextView = findViewById(R.id.difficultyText)
        commentsEditText = findViewById(R.id.commentsEditText)
        exportButton = findViewById(R.id.exportButton)
        importButton = findViewById(R.id.importButton)
        updatePercentagesButton = findViewById(R.id.updatePercentagesButton)
        addExerciseButton = findViewById(R.id.addExerciseButton)
        addMetricButton = findViewById(R.id.addMetricButton)
        deleteRowButton = findViewById(R.id.deleteRowButton)

        // Initialize the adapters and pass the activity as a callback
        exerciseAdapter = RecoveryPlanExerciseTableRowAdapter(exercises, this)
        metricAdapter = RecoveryPlanMetricTableRowAdapter(metrics, this)

        // Set up RecyclerView for exercise table
        exerciseTableLayout = findViewById(R.id.exerciseTableLayout)
        exerciseRecyclerView = findViewById(R.id.exerciseRecyclerView)
        exerciseRecyclerView.layoutManager = LinearLayoutManager(this)
        exerciseRecyclerView.adapter = exerciseAdapter
        ExerciseUtil.addItemDecorationToRecyclerView(exerciseRecyclerView)

        // Set up RecyclerView for metric table
        metricTableLayout = findViewById(R.id.metricTableLayout)
        metricRecyclerView = findViewById(R.id.metricRecyclerView)
        metricRecyclerView.layoutManager = LinearLayoutManager(this)
        metricRecyclerView.adapter = metricAdapter
        ExerciseUtil.addItemDecorationToRecyclerView(metricRecyclerView)

        // Set up date picker
        datePickerButton.setOnClickListener { showDatePicker() }

        difficultyProgressBar.setOnTouchListener { v, event ->
            if (event.action == MotionEvent.ACTION_DOWN || event.action == MotionEvent.ACTION_MOVE) {
                // Get the width of the progress bar
                val progressBarWidth = v.width

                // Calculate the touched position as a fraction of the progress bar's width
                val touchX = event.x.coerceIn(0f, progressBarWidth.toFloat()) // Ensure it stays within bounds
                val progressFraction = touchX / progressBarWidth

                // Map the fraction to the range of 1-10
                val progressValue = (progressFraction * 10).toInt().coerceIn(1, 10)

                // Update the progress bar
                difficultyProgressBar.progress = progressValue
                difficultyTextView.text = "$progressValue/10"
                v.performClick()
                saveDifficultyAndCommentsDateData()

                // Return true to indicate the event has been handled
                true
            } else {
                false
            }
        }

        // Handle saving general comments text
        commentsEditText.addTextChangedListener {
            saveDifficultyAndCommentsDateData()
        }

        // Handle import button click
        importButton.setOnClickListener { importDataFromExcel() }

        // Handle export button click
        exportButton.setOnClickListener { exportDataToExcel() }

        // Handle update percentages button click
        updatePercentagesButton.setOnClickListener {
            val currentWeek = dateTextView.text.toString()
            calculateExerciseCompletionForAllRows(this, exerciseAdapter, getExerciseGoals(exerciseAdapter), currentWeek)
            calculateMetricCompletionForAllRows(this, metricAdapter, getMetricGoals(metricAdapter), currentWeek)
        }

        // Handle add exercise button click
        addExerciseButton.setOnClickListener { showAddExerciseDialog() }

        // Handle add metric button click
        addMetricButton.setOnClickListener { showAddMetricDialog() }

        // Handle delete row button click
        deleteRowButton.setOnClickListener { showDeleteRowDialog() }

        // Load data for current week on activity start
        val currentWeek = calculateWeekRange(Calendar.getInstance())
        dateTextView.text = currentWeek
        loadWeekData(currentWeek)

        // Load data for exercise goal completion percentages
        calculateExerciseCompletionForAllRows(this, exerciseAdapter, getExerciseGoals(exerciseAdapter), currentWeek)
        calculateMetricCompletionForAllRows(this, metricAdapter, getMetricGoals(metricAdapter), currentWeek)

        // todo: fix import/export
        importButton.visibility = View.GONE
        exportButton.visibility = View.GONE
    }

    override fun onResume() {
        super.onResume()

        // Load data for current week on resuming
        val currentWeek = dateTextView.text.toString()
        loadWeekData(currentWeek)

        // Update visibility of recycler view tables
        updateExerciseTableVisibility()
        updateMetricTableVisibility()
    }

    // Save exercise data for the selected date to the apps storage
    override fun saveCurrentDateExerciseData() {
        val week = dateTextView.text.toString()
        val exercises = getExerciseGoals(exerciseAdapter)
        ExerciseDataStore(this, RECOVERY_PLAN_PREFERENCE).saveExercisesForDate(week, exercises)
    }

    // Save metric data for the selected date to the apps storage
    override fun saveCurrentDateMetricData() {
        val week = dateTextView.text.toString()
        val metrics = getMetricGoals(metricAdapter)
        ExerciseDataStore(this, RECOVERY_PLAN_PREFERENCE).saveMetricsForDate(week, metrics)
    }

    override fun onFocusFrequencyText(exercise: Exercise, position: Int) {
        showSetExerciseFrequencyDialog(exercise, position)
    }

    override fun onFocusFrequencyText(metric: Metric, position: Int) {
        showSetMetricFrequencyDialog(metric, position)
    }

    override fun onClickStartExerciseWithWarning(exercise: Exercise) {
        showStartExerciseWarningDialog(exercise)
    }

    override fun onClickStartMetricWithWarning(metric: Metric) {
        showStartMetricWarningDialog(metric)
    }

    override fun onClickStartExerciseWithoutWarning(exercise: Exercise) {
        onStartExerciseActivity(this, exercise)
    }

    override fun onClickStartMetricWithoutWarning(metric: Metric) {
        onStartMetricActivity(this, metric)
    }

    override fun onStartExerciseActivity(context: Context, exercise: Exercise) {
        val startExerciseIntent = Intent(context, StartExerciseActivity::class.java)
        val parcelableExercise = exercise as Parcelable
        startExerciseIntent.putExtra(ExerciseInfo.EXERCISE_KEY, parcelableExercise)
        ContextCompat.startActivity(context, startExerciseIntent, null)
    }

    override fun onStartMetricActivity(context: Context, metric: Metric) {
        val startMetricIntent = Intent(context, StartMetricActivity::class.java)
        val parcelableMetric = metric as Parcelable
        startMetricIntent.putExtra(ExerciseInfo.METRIC_KEY, parcelableMetric)
        ContextCompat.startActivity(context, startMetricIntent, null)
    }

    override fun onClickViewAllMetricDetails(metric: Metric) {
        // todo: show pop-up of an average of saved metric details for that week
    }

    override fun onDeleteRow() {
        deleteRow()
    }

    fun onAddExerciseRow(exercise: Exercise) {
        addExerciseRow(exercise)
    }

    fun onAddMetricRow(metric: Metric) {
        addMetricRow(metric)
    }

    fun getExerciseGoals(adapter: RecoveryExerciseAdapter): List<Exercise> {
        return adapter.getExercises()
    }

    fun getMetricGoals(adapter: RecoveryMetricAdapter): List<Metric> {
        return adapter.getMetrics()
    }

    fun calculateWeeklyProgress(context: Context, exerciseAdapter: RecoveryExerciseAdapter, metricAdapter: RecoveryMetricAdapter, exerciseGoals: List<Exercise>, metricGoals: List<Metric>, week: String): Double {
        val exerciseProgress = calculateExerciseCompletionForAllRows(context, exerciseAdapter, exerciseGoals, week)
        val metricProgress = calculateMetricCompletionForAllRows(context, metricAdapter, metricGoals, week)

        return if (exerciseGoals.isEmpty() && metricGoals.isEmpty()) {
            0.0
        } else {
            (exerciseProgress + metricProgress) / (exerciseGoals.size + metricGoals.size)
        }
    }

    fun calculateWeekRange(calendar: Calendar): String {
        // Adjust to the Sunday of the selected week
        val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
        val daysToSubtract = dayOfWeek - Calendar.SUNDAY
        calendar.add(Calendar.DATE, -daysToSubtract) // Move to the start of the week (Sunday)
        val startOfWeek = calendar.time

        // Calculate the Saturday of the same week
        calendar.add(Calendar.DATE, 6)
        val endOfWeek = calendar.time

        // Format dates into strings
        val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        val startDateString = dateFormat.format(startOfWeek)
        val endDateString = dateFormat.format(endOfWeek)

        // Display the date range in the TextView
        return "$startDateString - $endDateString"
    }

    // Load exercise data for the selected week from the apps storage
    fun loadExerciseWeekData(context: Context, adapter: RecoveryExerciseAdapter, week: String) {
        val exercises = ExerciseDataStore(context, RECOVERY_PLAN_PREFERENCE).getExercisesForDate(week)
        adapter.setExercises(exercises)
    }

    // Load metric data for the selected week from the apps storage
    fun loadMetricWeekData(context: Context, adapter: RecoveryMetricAdapter, week: String) {
        val metrics = ExerciseDataStore(context, RECOVERY_PLAN_PREFERENCE).getMetricsForDate(week)
        adapter.setMetrics(metrics)
    }

    private fun deleteRow() {
        exerciseAdapter.deleteExerciseRow()
        metricAdapter.deleteMetricRow()

        // Save data after deleting rows
        saveCurrentDateExerciseData()
        saveCurrentDateMetricData()

        // Update visibility of recycler view tables
        updateExerciseTableVisibility()
        updateMetricTableVisibility()
    }

    private fun addExerciseRow(exercise: Exercise) {
        exerciseAdapter.addExerciseRow(exercise)
        saveCurrentDateExerciseData() // Save data after adding new row

        // Fill details of exercise completion
        val week = dateTextView.text.toString()
        calculateExerciseCompletionForRow(this, exerciseAdapter, exercise, week)

        // Update visibility of recycler view table
        updateExerciseTableVisibility()
    }

    private fun addMetricRow(metric: Metric) {
        metricAdapter.addMetricRow(metric)
        saveCurrentDateMetricData() // Save data after adding new row

        // Fill details of metric completion
        val week = dateTextView.text.toString()
        calculateMetricCompletionForRow(this, metricAdapter, metric, week)

        // Update visibility of recycler view table
        updateMetricTableVisibility()
    }

    private fun showDatePicker() {
        // Get current date
        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)

        val datePickerDialog = DatePickerDialog(
            this,
            { _, selectedYear, selectedMonth, selectedDay ->
                // Set the calendar to the selected date
                val selectedCalendar = Calendar.getInstance()
                selectedCalendar.set(selectedYear, selectedMonth, selectedDay)

                val chosenWeek = calculateWeekRange(selectedCalendar)
                dateTextView.text = chosenWeek

                // Load data for the selected week
                loadWeekData(chosenWeek)

                // Recalculate percentages for that week
                calculateWeeklyProgress(this, exerciseAdapter, metricAdapter, getExerciseGoals(exerciseAdapter), getMetricGoals(metricAdapter), chosenWeek)
            },
            year,
            month,
            day
        )
        datePickerDialog.show()
    }

    // Load data from the selected week from the apps storage
    @SuppressLint("SetTextI18n")
    private fun loadWeekData(week: String) {
        // Load the general difficulty & comments for the week
        val difficultyAndCommentsPair = ExerciseDataStore(this, RECOVERY_PLAN_PREFERENCE).getDifficultyAndCommentsPairForDate(week)
        difficultyProgressBar.progress = difficultyAndCommentsPair.first
        difficultyTextView.text = "${difficultyAndCommentsPair.first}/10"
        commentsEditText.setText(difficultyAndCommentsPair.second)

        // Load the exercises for the week
        loadExerciseWeekData(this, exerciseAdapter, week)

        // Load the metrics for the week
        loadMetricWeekData(this, metricAdapter, week)

        // Update visibility of recycler view tables
        updateExerciseTableVisibility()
        updateMetricTableVisibility()
    }

    // Save difficulty & comments data for the selected week to the apps storage
    private fun saveDifficultyAndCommentsDateData() {
        val week = dateTextView.text.toString()
        val difficulty = difficultyProgressBar.progress
        val comments = commentsEditText.text.toString()
        ExerciseDataStore(this, RECOVERY_PLAN_PREFERENCE).saveDifficultyAndCommentsPairForDate(week, difficulty, comments)
    }

    // Show pop-up dialog for adding an exercise goal row to the table
    private fun showAddExerciseDialog() {
        val addExerciseGoalRowFragment = AddExerciseGoalRowFragment(this, exerciseAdapter)
        addExerciseGoalRowFragment.show(supportFragmentManager, "add_exercise_goal_row")
    }

    // Show pop-up dialog for adding a metric goal row to the table
    private fun showAddMetricDialog() {
        val addMetricGoalRowFragment = AddMetricGoalRowFragment(this, metricAdapter)
        addMetricGoalRowFragment.show(supportFragmentManager, "add_metric_goal_row")
    }

    // Show pop-up dialog for deleting an exercise/metric row from the table
    private fun showDeleteRowDialog() {
        val deleteRowFragment = DeleteRowFragment()
        deleteRowFragment.show(supportFragmentManager, "delete_row")
    }

    // Show pop-up dialog for setting exercise goal frequency for a row
    private fun showSetExerciseFrequencyDialog(exercise: Exercise, position: Int) {
        val addGoalFreqFragment = AddGoalFreqFragment(this, exerciseAdapter = exerciseAdapter, exercise = exercise, position = position)
        addGoalFreqFragment.show(supportFragmentManager, "add_goal_freq")
    }

    // Show pop-up dialog for setting metric goal frequency for a row
    private fun showSetMetricFrequencyDialog(metric: Metric, position: Int) {
        val addGoalFreqFragment = AddGoalFreqFragment(this, metricAdapter = metricAdapter, metric = metric, position = position)
        addGoalFreqFragment.show(supportFragmentManager, "add_goal_freq")
    }

    // Show pop-up dialog for warning of starting an exercise that has above 100% completion
    private fun showStartExerciseWarningDialog(exercise: Exercise) {
        val startExerciseWarningFragment = StartExerciseWarningFragment(this, exercise = exercise)
        startExerciseWarningFragment.show(supportFragmentManager, "start_exercise_warning")
    }

    // Show pop-up dialog for warning of starting an metric that has above 100% completion
    private fun showStartMetricWarningDialog(metric: Metric) {
        val startExerciseWarningFragment = StartExerciseWarningFragment(this, metric = metric)
        startExerciseWarningFragment.show(supportFragmentManager, "start_metric_warning")
    }

    // Calculate the % completed value for all exercise goal rows
    private fun calculateExerciseCompletionForAllRows(context: Context, exerciseAdapter: RecoveryExerciseAdapter, exerciseGoals: List<Exercise>, week: String): Double {
        var totalPercentageCompleted = 0.0

        // Loop through each exercise in the Recovery Plan table
        for (exercise in exerciseGoals) {
            var percentageCompleted = calculateExerciseCompletionForRow(context, exerciseAdapter, exercise, week)

            if (percentageCompleted > 100) {
                percentageCompleted = 100.0
            }
            totalPercentageCompleted += percentageCompleted
        }

        return if (exerciseGoals.isEmpty()) 0.0 else totalPercentageCompleted
    }

    // Calculate the % completed value for all metric goal rows
    private fun calculateMetricCompletionForAllRows(context: Context, metricAdapter: RecoveryMetricAdapter, metricGoals: List<Metric>, week: String): Double {
        var totalPercentageCompleted = 0.0

        // Loop through each metric in the Recovery Plan table
        for (metric in metricGoals) {
            var percentageCompleted = calculateMetricCompletionForRow(context, metricAdapter, metric, week)

            if (percentageCompleted > 100) {
                percentageCompleted = 100.0
            }
            totalPercentageCompleted += percentageCompleted
        }

        return if (metricGoals.isEmpty()) 0.0 else totalPercentageCompleted
    }

    // Calculate the % completed value for a single exercise goal row
    private fun calculateExerciseCompletionForRow(context: Context, exerciseAdapter: RecoveryExerciseAdapter, exerciseGoal: Exercise, week: String): Double {
        val exerciseName = exerciseGoal.name

        // Fetch the completed data for this exercise from the Recovery Data table
        val recoveryDataExerciseList = fetchRecoveryDataForExerciseGoal(context, exerciseName, week)

        // Calculate the total completed value from the Recovery Data table
        var totalCompletedValue = 0
        for (recoveryDataExercise in recoveryDataExerciseList) {
            totalCompletedValue += recoveryDataExercise.sets * recoveryDataExercise.reps *
                    recoveryDataExercise.hold * recoveryDataExercise.tension
        }

        // Calculate the goal value from the Recovery Plan table row
        val freqFactors = exerciseGoal.frequency.split("x/")
        val freqNumber = freqFactors[0].toInt()
        val freqCategory = when (freqFactors[1]) {
            "day" -> 7
            "week" -> 1
            else -> 1
        }
        val goalValue = exerciseGoal.sets * exerciseGoal.reps * exerciseGoal.hold * exerciseGoal.tension * freqNumber * freqCategory

        // Calculate the percentage of completion
        var percentageCompleted = 0.0
        if (goalValue != 0) {
            percentageCompleted = (totalCompletedValue.toDouble() / goalValue) * 100
        }

        // Update the exercise data
        val previousPercentage = exerciseGoal.percentageCompleted
        exerciseGoal.percentageCompleted = percentageCompleted

        // Notify the adapter to update the UI for the row if it has changed
        if (previousPercentage != percentageCompleted) {
            val position = exercises.indexOf(exerciseGoal) + 1 // +1 to account for the header
            exerciseAdapter.notifyItemChangedAndRefresh(position)
        }

        return percentageCompleted
    }

    // Calculate the % completed value for a single metric goal row
    private fun calculateMetricCompletionForRow(context: Context, metricAdapter: RecoveryMetricAdapter, metricGoal: Metric, week: String): Double {
        val metricName = metricGoal.name

        // Fetch the completed data for this metric from the Recovery Data table
        val recoveryDataMetricList = fetchRecoveryDataForMetricGoal(context, metricName, week)

        // Calculate the total completed value from the Recovery Data table
        val totalCompletedValue = recoveryDataMetricList.size

        // Calculate the goal value from the Recovery Plan table row
        val freqFactors = metricGoal.frequency.split("x/")
        val freqNumber = freqFactors[0].toInt()
        val freqCategory = when (freqFactors[1]) {
            "day" -> 7
            "week" -> 1
            else -> 1
        }
        val goalValue = freqNumber * freqCategory

        // Calculate the percentage of completion
        var percentageCompleted = 0.0
        if (goalValue != 0) {
            percentageCompleted = (totalCompletedValue.toDouble() / goalValue) * 100
        }

        // Update the metric data
        val previousPercentage = metricGoal.percentageCompleted
        metricGoal.percentageCompleted = percentageCompleted

        // Notify the adapter to update the UI for the row if it has changed
        if (previousPercentage != percentageCompleted) {
            val position = metrics.indexOf(metricGoal) + 1 // +1 to account for the header
            metricAdapter.notifyItemChangedAndRefresh(position)
        }

        return percentageCompleted
    }

    // Fetch exercise data from Recovery Data table with corresponding exercise name
    private fun fetchRecoveryDataForExerciseGoal(context: Context, exerciseName: String, week: String): List<Exercise> {
        // Fetch all exercises with same name for each day of the week
        val firstDateString = week.split(" - ")[0] // Get the first date (start of the week)

        // Parse the first date into a Calendar object
        val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        val calendar = Calendar.getInstance()
        calendar.time = dateFormat.parse(firstDateString) ?: throw IllegalArgumentException("Invalid date format")

        val exercises: MutableList<Exercise> = mutableListOf()

        // Loop over the 7 days in the week
        for (i in 0..6) {
            // Format the current date as a string
            val currentDate = dateFormat.format(calendar.time)

            // Fetch exercises for the current date and filter by exerciseName
            val dailyExercises = ExerciseDataStore(context, RECOVERY_DATA_PREFERENCE)
                .getExercisesForDate(currentDate)
                .filter { it.name == exerciseName }
            exercises.addAll(dailyExercises)

            // Move to the next day
            calendar.add(Calendar.DATE, 1)
        }

        return exercises
    }

    // Fetch metric data from Recovery Data table with corresponding metric name
    private fun fetchRecoveryDataForMetricGoal(context: Context, metricName: String, week: String): List<Metric> {
        // Fetch all metrics with same name for each day of the week
        val firstDateString = week.split(" - ")[0] // Get the first date (start of the week)

        // Parse the first date into a Calendar object
        val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        val calendar = Calendar.getInstance()
        calendar.time = dateFormat.parse(firstDateString) ?: throw IllegalArgumentException("Invalid date format")

        val metrics: MutableList<Metric> = mutableListOf()

        // Loop over the 7 days in the week
        for (i in 0..6) {
            // Format the current date as a string
            val currentDate = dateFormat.format(calendar.time)

            // Fetch metrics for the current date and filter by metricName
            val dailyMetrics = ExerciseDataStore(context, RECOVERY_DATA_PREFERENCE)
                .getMetricsForDate(currentDate)
                .filter { it.name == metricName }
            metrics.addAll(dailyMetrics)

            // Move to the next day
            calendar.add(Calendar.DATE, 1)
        }

        return metrics
    }

    // Update the visibility of the exercise table
    private fun updateExerciseTableVisibility() {
        // Update visibility of recycler view table after the adapter processes the data
        exerciseRecyclerView.post {
            ExerciseUtil.updateRecyclerViewVisibility(exerciseAdapter, exerciseTableLayout)
        }
    }

    // Update the visibility of the metric table
    private fun updateMetricTableVisibility() {
        // Update visibility of recycler view table after the adapter processes the data
        metricRecyclerView.post {
            ExerciseUtil.updateRecyclerViewVisibility(metricAdapter, metricTableLayout)
        }
    }

    //todo: fix export/import functions for the table

    // Function to export table data to Excel
    private fun exportDataToExcel() {
        val date = dateTextView.text.toString()
        val exercises = getExerciseGoals(exerciseAdapter)

        try {
            val fileName = "RecoveryPlan_$date.csv"
            val file = File(getExternalFilesDir(null), fileName)
            val writer = file.bufferedWriter()

            writer.write("Exercise,Sets,Reps,Hold,Tension,Frequency,Difficulty,Comments,Started\n")
            exercises.forEach { exercise ->
                writer.write("${exercise.name},${exercise.sets},${exercise.reps},${exercise.hold}," +
                        "${exercise.tension},${exercise.frequency},${exercise.difficulty}," +
                        "${exercise.comments},${exercise.isSelected}\n")
            }

            writer.close()
            // Notify user
            GeneralUtil.showToast(this, layoutInflater, "Data exported to $fileName")
        } catch (e: Exception) {
            e.printStackTrace()
            GeneralUtil.showToast(this, layoutInflater, "Export failed")
        }
    }

    // Function to import table data from Excel
    private fun importDataFromExcel() {
        val date = dateTextView.text.toString()
        try {
            val file = File(getExternalFilesDir(null), "RecoveryPlan_$date.csv")
            if (!file.exists()) {
                GeneralUtil.showToast(this, layoutInflater, "No file found to import.")
                return
            }

            val exercises = mutableListOf<Exercise>()
            file.forEachLine { line ->
                val columns = line.split(",")
                if (columns[0] == "Exercise") return@forEachLine // Skip header

                exercises.add(
                    Exercise(
                        id = ExerciseUtil.generateNewExerciseId(exercises),
                        name = columns[0],
                        sets = columns[1].toInt(),
                        reps = columns[2].toInt(),
                        hold = columns[3].toInt(),
                        tension = columns[4].toInt(),
                        frequency = columns[5],
                        comments = columns[7],
                        isSelected = columns[8].toBoolean()
                    )
                )
            }

            exerciseAdapter.setExercises(exercises)
            GeneralUtil.showToast(this, layoutInflater, "Data imported successfully.")
        } catch (e: Exception) {
            e.printStackTrace()
            GeneralUtil.showToast(this, layoutInflater, "Import failed")
        }
    }
}
