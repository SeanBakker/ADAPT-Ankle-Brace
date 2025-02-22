package com.example.adaptanklebrace

import android.annotation.SuppressLint
import android.app.DatePickerDialog
import android.content.res.ColorStateList
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.PopupWindow
import android.widget.ProgressBar
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.core.widget.addTextChangedListener
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.adaptanklebrace.adapters.RecoveryDataExerciseTableRowAdapter
import com.example.adaptanklebrace.adapters.RecoveryDataMetricTableRowAdapter
import com.example.adaptanklebrace.data.Exercise
import com.example.adaptanklebrace.data.Metric
import com.example.adaptanklebrace.fragments.AddExerciseDataRowFragment
import com.example.adaptanklebrace.fragments.AddMetricDataRowFragment
import com.example.adaptanklebrace.fragments.ChooseActivityTypeFragment
import com.example.adaptanklebrace.fragments.DeleteRowFragment
import com.example.adaptanklebrace.utils.ExerciseDataStore
import com.example.adaptanklebrace.utils.ExerciseUtil
import com.example.adaptanklebrace.utils.GeneralUtil
import java.io.File
import java.text.SimpleDateFormat
import java.time.LocalTime
import java.util.*

class RecoveryDataActivity : BaseActivity(), RecoveryDataExerciseTableRowAdapter.RecoveryDataCallback,
    RecoveryDataMetricTableRowAdapter.RecoveryDataCallback, DeleteRowFragment.OnDeleteListener,
    ChooseActivityTypeFragment.ChooseActivityTypeListener {

    private lateinit var dateTextView: TextView
    private lateinit var datePickerButton: Button
    private lateinit var difficultyProgressBar: ProgressBar
    private lateinit var difficultyTextView: TextView
    private lateinit var commentsEditText: EditText
    private lateinit var exportButton: Button
    private lateinit var importButton: Button
    private lateinit var deleteRowButton: Button
    private lateinit var addRowButton: Button

    // Exercise table variables
    private lateinit var exerciseTableLayout: ConstraintLayout
    private lateinit var exerciseRecyclerView: RecyclerView
    private lateinit var exerciseAdapter: RecoveryDataExerciseTableRowAdapter
    private var exercises: MutableList<Exercise> = mutableListOf()

    // Metric table variables
    private lateinit var metricTableLayout: ConstraintLayout
    private lateinit var metricRecyclerView: RecyclerView
    private lateinit var metricAdapter: RecoveryDataMetricTableRowAdapter
    private var metrics: MutableList<Metric> = mutableListOf()

    companion object {
        const val RECOVERY_DATA_PREFERENCE = "RecoveryData"
    }

    @SuppressLint("ClickableViewAccessibility", "SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_recovery_data)

        // Initialize views
        dateTextView = findViewById(R.id.dateText)
        datePickerButton = findViewById(R.id.datePickerButton)
        difficultyProgressBar = findViewById(R.id.difficultyProgressBar)
        difficultyProgressBar.progressTintList = ColorStateList.valueOf(ContextCompat.getColor(this, R.color.red_1))
        difficultyTextView = findViewById(R.id.difficultyText)
        commentsEditText = findViewById(R.id.commentsEditText)
        exportButton = findViewById(R.id.exportButton)
        importButton = findViewById(R.id.importButton)
        addRowButton = findViewById(R.id.addRowButton)
        deleteRowButton = findViewById(R.id.deleteRowButton)

        // Initialize the adapters and pass the activity as a callback
        exerciseAdapter = RecoveryDataExerciseTableRowAdapter(exercises, this)
        metricAdapter = RecoveryDataMetricTableRowAdapter(metrics, this)

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

        // Handle export button click
        exportButton.setOnClickListener { exportDataToExcel() }

        // Handle import button click
        importButton.setOnClickListener { importDataFromExcel() }

        // Handle add row button click
        addRowButton.setOnClickListener { showChooseActivityTypeDialog() }

        // Handle delete exercise button click
        deleteRowButton.setOnClickListener { showDeleteRowDialog() }

        // Load data for today's date on activity start
        val currentDate = GeneralUtil.getCurrentDate()
        dateTextView.text = currentDate
        loadDateData(currentDate)

        // todo: fix import/export
        importButton.visibility = View.GONE
        exportButton.visibility = View.GONE
    }

    override fun onResume() {
        super.onResume()

        // Update visibility of recycler view tables
        updateExerciseTableVisibility()
        updateMetricTableVisibility()
    }

    // Save exercise data for the selected date to the apps storage
    override fun saveCurrentDateExerciseData() {
        val date = dateTextView.text.toString()
        val exercises = exerciseAdapter.getExercises()
        ExerciseDataStore(this, RECOVERY_DATA_PREFERENCE).saveExercisesForDate(date, exercises)
    }

    // Save metric data for the selected date to the apps storage
    override fun saveCurrentDateMetricData() {
        val date = dateTextView.text.toString()
        val metrics = metricAdapter.getMetrics()
        ExerciseDataStore(this, RECOVERY_DATA_PREFERENCE).saveMetricsForDate(date, metrics)
    }

    @SuppressLint("DefaultLocale", "InflateParams")
    override fun onClickViewROMMetricDetails(metric: Metric, view: View) {
        // Inflate the popup layout
        val inflater = LayoutInflater.from(this)
        val popupView = inflater.inflate(R.layout.dialog_view_rom_metric_details, null)

        // Set metric data in the popup
        popupView.findViewById<TextView>(R.id.romPlantarDorsiflexionRange).text =
            String.format("%.1f°", metric.romPlantarDorsiflexionRange)
        popupView.findViewById<TextView>(R.id.romInversionEversionRange).text =
            String.format("%.1f°", metric.romInversionEversionRange)

        // Create a PopupWindow
        val popupWindow = PopupWindow(
            popupView,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            true
        )
        // Set background drawable
        popupWindow.setBackgroundDrawable(ContextCompat.getDrawable(this, R.drawable.dialog_view_metric_background))
        popupWindow.isClippingEnabled = false

        // Show the popup directly under the clicked button
        popupWindow.showAsDropDown(view, -85, -10) // Position popup below the button
    }

    @SuppressLint("DefaultLocale", "InflateParams")
    override fun onClickViewGaitMetricDetails(metric: Metric, view: View) {
        // Inflate the popup layout
        val inflater = LayoutInflater.from(this)
        val popupView = inflater.inflate(R.layout.dialog_view_gait_metric_details, null)

        // Set metric data in the popup
        popupView.findViewById<TextView>(R.id.gaitNumSteps).text =
            String.format("%d", metric.gaitNumSteps)
        popupView.findViewById<TextView>(R.id.gaitCadence).text =
            String.format("%.1f", metric.gaitCadence)
        popupView.findViewById<TextView>(R.id.gaitImpactForce).text =
            String.format("%.1f", metric.gaitImpactForce)
        popupView.findViewById<TextView>(R.id.gaitSwingStanceRatio).text =
            String.format("%.1f", metric.gaitSwingStanceRatio)

        // Create a PopupWindow
        val popupWindow = PopupWindow(
            popupView,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            true
        )
        // Set background drawable
        popupWindow.setBackgroundDrawable(ContextCompat.getDrawable(this, R.drawable.dialog_view_metric_background))
        popupWindow.isClippingEnabled = false

        // Show the popup directly under the clicked button
        popupWindow.showAsDropDown(view, -60, -10) // Position popup below the button
    }

    override fun onChooseActivityExercise() {
        showAddExerciseDialog()
    }

    override fun onChooseActivityMetric() {
        showAddMetricDialog()
    }

    override fun onDeleteRow() {
        deleteRow()
    }

    override fun onDestroy() {
        // Save current state of the data
        saveCurrentDateExerciseData()
        saveCurrentDateMetricData()

        super.onDestroy()
    }

    fun onAddRow(exercise: Exercise) {
        addExerciseRow(exercise)
    }

    fun onAddRow(metric: Metric) {
        addMetricRow(metric)
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

        // Update visibility of recycler view table
        updateExerciseTableVisibility()
    }

    private fun addMetricRow(metric: Metric) {
        metricAdapter.addMetricRow(metric)
        saveCurrentDateMetricData() // Save data after adding new row

        // Update visibility of recycler view table
        updateMetricTableVisibility()
    }

    private fun showDatePicker() {
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
                val selectedDate = selectedCalendar.time

                val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                val formattedDate = dateFormat.format(selectedDate)
                dateTextView.text = formattedDate

                // Load data for the selected date
                loadDateData(formattedDate)
            },
            year,
            month,
            day
        )
        datePickerDialog.show()
    }

    // Load data from the selected date from the apps storage
    @SuppressLint("SetTextI18n")
    private fun loadDateData(date: String) {
        // Load the general difficulty & comments for the date
        val difficultyAndCommentsPair = ExerciseDataStore(this, RECOVERY_DATA_PREFERENCE).getDifficultyAndCommentsPairForDate(date)
        difficultyProgressBar.progress = difficultyAndCommentsPair.first
        difficultyTextView.text = "${difficultyAndCommentsPair.first}/10"
        commentsEditText.setText(difficultyAndCommentsPair.second)

        // Load the exercises for the date
        val exercises = ExerciseDataStore(this, RECOVERY_DATA_PREFERENCE).getExercisesForDate(date)
        exerciseAdapter.setExercises(exercises)

        // Load the metrics for the date
        val metrics = ExerciseDataStore(this, RECOVERY_DATA_PREFERENCE).getMetricsForDate(date)
        metricAdapter.setMetrics(metrics)

        // Update visibility of recycler view tables
        updateExerciseTableVisibility()
        updateMetricTableVisibility()
    }

    // Save difficulty & comments data for the selected date to the apps storage
    private fun saveDifficultyAndCommentsDateData() {
        val date = dateTextView.text.toString()
        val difficulty = difficultyProgressBar.progress
        val comments = commentsEditText.text.toString()
        ExerciseDataStore(this, RECOVERY_DATA_PREFERENCE).saveDifficultyAndCommentsPairForDate(date, difficulty, comments)
    }

    // Show pop-up dialog for choosing activity type (exercise or metric)
    private fun showChooseActivityTypeDialog() {
        val chooseActivityTypeFragment = ChooseActivityTypeFragment()
        chooseActivityTypeFragment.show(supportFragmentManager, "choose_activity_type")
    }

    // Show pop-up dialog for adding an exercise data row to the table
    private fun showAddExerciseDialog() {
        val addExerciseDataRowFragment = AddExerciseDataRowFragment(this, exerciseAdapter)
        addExerciseDataRowFragment.show(supportFragmentManager, "add_exercise_data_row")
    }

    // Show pop-up dialog for adding a metric data row to the table
    private fun showAddMetricDialog() {
        val addMetricDataRowFragment = AddMetricDataRowFragment(this, metricAdapter)
        addMetricDataRowFragment.show(supportFragmentManager, "add_metric_data_row")
    }

    // Show pop-up dialog for deleting an exercise/metric row from the table
    private fun showDeleteRowDialog() {
        val deleteRowFragment = DeleteRowFragment()
        deleteRowFragment.show(supportFragmentManager, "delete_row")
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
        val exercises = exerciseAdapter.getExercises()

        try {
            val fileName = "RecoveryData_$date.csv"
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
            GeneralUtil.showToast(this, layoutInflater, getString(R.string.exportFailedToast))
        }
    }

    // Function to import table data from Excel
    private fun importDataFromExcel() {
        val date = dateTextView.text.toString()
        try {
            val file = File(getExternalFilesDir(null), "RecoveryData_$date.csv")
            if (!file.exists()) {
                GeneralUtil.showToast(this, layoutInflater, getString(R.string.importNoFileToast))
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
                        timeCompleted = LocalTime.parse(columns[5], GeneralUtil.timeFormatter),
                        difficulty = columns[6].toInt(),
                        comments = columns[7],
                        isSelected = columns[8].toBoolean()
                    )
                )
            }

            exerciseAdapter.setExercises(exercises)
            GeneralUtil.showToast(this, layoutInflater, getString(R.string.importSuccessToast))
        } catch (e: Exception) {
            e.printStackTrace()
            GeneralUtil.showToast(this, layoutInflater, getString(R.string.importFailedToast))
        }
    }
}
