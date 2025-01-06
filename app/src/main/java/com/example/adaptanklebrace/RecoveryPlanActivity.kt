package com.example.adaptanklebrace

import android.app.DatePickerDialog
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.adaptanklebrace.RecoveryDataActivity.Companion.RECOVERY_DATA_PREFERENCE
import com.example.adaptanklebrace.adapters.RecoveryPlanTableRowAdapter
import com.example.adaptanklebrace.data.Exercise
import com.example.adaptanklebrace.fragments.AddExerciseGoalRowFragment
import com.example.adaptanklebrace.fragments.DeleteRowFragment
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

@RequiresApi(Build.VERSION_CODES.Q)
class RecoveryPlanActivity : AppCompatActivity(), RecoveryPlanTableRowAdapter.SaveDataCallback,
    DeleteRowFragment.OnDeleteListener {

    private lateinit var dateTextView: TextView
    private lateinit var datePickerButton: Button
    private lateinit var difficultyProgressBar: ProgressBar
    private lateinit var commentsEditText: EditText
    private lateinit var exerciseRecyclerView: RecyclerView
    private lateinit var exportButton: Button
    private lateinit var importButton: Button
    private lateinit var updatePercentagesButton: Button
    private lateinit var addExerciseButton: Button
    private lateinit var deleteExerciseButton: Button

    private lateinit var exerciseAdapter: RecoveryPlanTableRowAdapter
    private var exercises: MutableList<Exercise> = mutableListOf()

    companion object {
        const val RECOVERY_PLAN_PREFERENCE = "RecoveryPlan"
    }

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
            @Suppress("DEPRECATION")
            onBackPressed()
        }

        // Initialize views
        dateTextView = findViewById(R.id.dateText)
        datePickerButton = findViewById(R.id.datePickerButton)
        difficultyProgressBar = findViewById(R.id.difficultyProgressBar)
        commentsEditText = findViewById(R.id.commentsEditText)
        exerciseRecyclerView = findViewById(R.id.exerciseRecyclerView)
        exportButton = findViewById(R.id.exportButton)
        importButton = findViewById(R.id.importButton)
        updatePercentagesButton = findViewById(R.id.updatePercentagesButton)
        addExerciseButton = findViewById(R.id.addExerciseButton)
        deleteExerciseButton = findViewById(R.id.deleteExerciseButton)

        // Initialize the adapter and pass the activity as a callback
        exerciseAdapter = RecoveryPlanTableRowAdapter(exercises, this)

        // Set up RecyclerView
        exerciseRecyclerView.layoutManager = LinearLayoutManager(this)
        exerciseRecyclerView.adapter = exerciseAdapter

        // Set up date picker
        datePickerButton.setOnClickListener { showDatePicker() }

        // Handle import button click
        importButton.setOnClickListener { importDataFromExcel() }

        // Handle export button click
        exportButton.setOnClickListener { exportDataToExcel() }

        // Handle update percentages button click
        updatePercentagesButton.setOnClickListener {
            calculateExerciseCompletionForAllRows(this, getExerciseGoals(), dateTextView.text.toString())
        }

        // Handle add exercise button click
        addExerciseButton.setOnClickListener { showAddExerciseDialog() }

        // Handle delete exercise button click
        deleteExerciseButton.setOnClickListener { showDeleteExerciseDialog() }

        // Load data for current week on activity start
        val currentWeek = calculateWeekRange(Calendar.getInstance())
        dateTextView.text = currentWeek
        loadWeekData(currentWeek)

        // Get the list of all exercise goals from the adapter
        val exerciseGoals = getExerciseGoals()

        // Load data for exercise goal completion percentages
        calculateExerciseCompletionForAllRows(this, exerciseGoals, currentWeek)
    }

    override fun saveCurrentDateData() {
        val week = dateTextView.text.toString()
        val exercises = getExerciseGoals()
        ExerciseDataStore(this, RECOVERY_PLAN_PREFERENCE).saveExercisesForDate(week, exercises)
    }

    override fun onDeleteRow() {
        deleteExerciseRow()
    }

    fun onAddRow(exercise: Exercise) {
        addExerciseRow(exercise)
    }

    fun getExerciseGoals(): List<Exercise> {
        return exerciseAdapter.getExercises()
    }

    fun calculateWeeklyProgress(context: Context, exerciseGoals: List<Exercise>, week: String): Double {
        return calculateExerciseCompletionForAllRows(context, exerciseGoals, week, false)
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

    private fun deleteExerciseRow() {
        exerciseAdapter.deleteExerciseRow()
        saveCurrentDateData() // Save data after deleting rows
    }

    private fun addExerciseRow(exercise: Exercise) {
        exerciseAdapter.addExerciseRow(exercise)
        saveCurrentDateData() // Save data after adding new row

        // Fill details of exercise completion
        val week = dateTextView.text.toString()
        calculateExerciseCompletionForRow(this, exercise, week)
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
            },
            year,
            month,
            day
        )
        datePickerDialog.show()
    }

    private fun loadWeekData(week: String) {
        val exercises = ExerciseDataStore(this, RECOVERY_PLAN_PREFERENCE).getExercisesForDate(week)
        exerciseAdapter.setExercises(exercises)
    }

    // Show pop-up dialog for adding exercise goal row to the table
    private fun showAddExerciseDialog() {
        val addExerciseGoalRowFragment = AddExerciseGoalRowFragment()
        addExerciseGoalRowFragment.show(supportFragmentManager, "add_exercise_goal_row")
    }

    // Show pop-up dialog for deleting an exercise row from the table
    private fun showDeleteExerciseDialog() {
        val deleteRowFragment = DeleteRowFragment()
        deleteRowFragment.show(supportFragmentManager, "delete_row")
    }

    // Calculate the % completed value for all exercise goal rows
    private fun calculateExerciseCompletionForAllRows(context: Context, exerciseGoals: List<Exercise>, week: String, notifyAdapter: Boolean = true): Double {
        var totalPercentageCompleted = 0.0

        // Loop through each exercise in the Recovery Plan table
        for (exercise in exerciseGoals) {
            var percentageCompleted = calculateExerciseCompletionForRow(context, exercise, week, notifyAdapter)

            if (percentageCompleted > 100) {
                percentageCompleted = 100.0
            }
            totalPercentageCompleted += percentageCompleted
        }

        return if (exerciseGoals.isEmpty()) 0.0 else (totalPercentageCompleted / exerciseGoals.size)
    }

    // Calculate the % completed value for a single exercise goal row
    private fun calculateExerciseCompletionForRow(context: Context, exerciseGoal: Exercise, week: String, notifyAdapter: Boolean = true): Double {
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
        exerciseGoal.percentageCompleted = percentageCompleted

        // Notify the adapter to update the UI for the row
        if (notifyAdapter) {
            exerciseAdapter.notifyItemChanged(exercises.indexOf(exerciseGoal) + 1) // +1 to account for the header
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

    // Function to export table data to Excel
    private fun exportDataToExcel() {
        val date = dateTextView.text.toString()
        val exercises = getExerciseGoals()

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
            Toast.makeText(this, "Data exported to $fileName", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Export failed", Toast.LENGTH_SHORT).show()
        }
    }

    // Function to import table data from Excel
    private fun importDataFromExcel() {
        val date = dateTextView.text.toString()
        try {
            val file = File(getExternalFilesDir(null), "RecoveryPlan_$date.csv")
            if (!file.exists()) {
                Toast.makeText(this, "No file found to import.", Toast.LENGTH_SHORT).show()
                return
            }

            val exercises = mutableListOf<Exercise>()
            file.forEachLine { line ->
                val columns = line.split(",")
                if (columns[0] == "Exercise") return@forEachLine // Skip header

                exercises.add(
                    Exercise(
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
            Toast.makeText(this, "Data imported successfully.", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Import failed", Toast.LENGTH_SHORT).show()
        }
    }
}
