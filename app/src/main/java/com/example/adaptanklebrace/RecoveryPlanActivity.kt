package com.example.adaptanklebrace

import android.app.DatePickerDialog
import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.adaptanklebrace.adapters.ExerciseItemAdapter
import com.example.adaptanklebrace.adapters.RecoveryPlanTableRowAdapter
import com.example.adaptanklebrace.data.Exercise
import com.example.adaptanklebrace.enums.ExerciseType
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
    private lateinit var addExerciseButton: Button
    private lateinit var deleteExerciseButton: Button

    private lateinit var exerciseAdapter: RecoveryPlanTableRowAdapter
    private var exercises: MutableList<Exercise> = mutableListOf()

    private val RECOVERY_PLAN_PREFERENCE = "RecoveryPlan"

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
        addExerciseButton = findViewById(R.id.addExerciseButton)
        deleteExerciseButton = findViewById(R.id.deleteExerciseButton)

        // Initialize the adapter and pass the activity as a callback
        exerciseAdapter = RecoveryPlanTableRowAdapter(exercises, this)

        // Set up RecyclerView
        exerciseRecyclerView.layoutManager = LinearLayoutManager(this)
        exerciseRecyclerView.adapter = exerciseAdapter

        // Set up date picker
        datePickerButton.setOnClickListener { showDatePicker() }

        // Handle export button click
        exportButton.setOnClickListener { exportDataToExcel() }

        // Handle import button click
        importButton.setOnClickListener { importDataFromExcel() }

        // Handle add exercise button click
        addExerciseButton.setOnClickListener { addExerciseRow() }

        // Handle delete exercise button click
        deleteExerciseButton.setOnClickListener { showDeleteExerciseDialog() }

        // Load data for current week on activity start
        val currentWeek = calculateWeekRange(Calendar.getInstance())
        dateTextView.text = currentWeek
        loadWeekData(currentWeek)
    }

    override fun saveCurrentDateData() {
        val date = dateTextView.text.toString()
        val exercises = exerciseAdapter.getExercises()
        ExerciseDataStore(this, RECOVERY_PLAN_PREFERENCE).saveExercisesForDate(date, exercises)
    }

    override fun onDeleteRow() {
        deleteExerciseRow()
    }

    private fun deleteExerciseRow() {
        exerciseAdapter.deleteExerciseRow()
        saveCurrentDateData() // Save data after deleting rows
    }

    private fun addExerciseRow() {
        //showAddExerciseDialog() //todo: fix errors
        exerciseAdapter.addExerciseRow("test")
        saveCurrentDateData() // Save data after adding new row
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

    private fun calculateWeekRange(calendar: Calendar): String {
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

    private fun loadWeekData(week: String) {
        val exercises = ExerciseDataStore(this, RECOVERY_PLAN_PREFERENCE).getExercisesForDate(week)
        exerciseAdapter.setExercises(exercises)
    }

    // Show pop-up dialog for choosing exercise type to add to the table
    private fun showAddExerciseDialog() {
        val builder = AlertDialog.Builder(this)
        val inflater = layoutInflater
        val dialogView = inflater.inflate(R.layout.add_exercise_row_fragment, null)
        builder.setView(dialogView)

        val customExerciseName: EditText = dialogView.findViewById(R.id.customExerciseName)

        // Set up RecyclerView for the exercise list
        val recyclerView: RecyclerView = dialogView.findViewById(R.id.exerciseRecyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)

        // List of exercises (can be customized)
        val exerciseNames = ExerciseType.values().map { it.name }

        // Set up adapter for RecyclerView
        val adapter = ExerciseItemAdapter(exerciseNames) { selectedExercise ->
            // Handle exercise selection
            customExerciseName.setText(selectedExercise)
        }
        recyclerView.adapter = adapter

        builder.setPositiveButton("Add") { _, _ ->
            val selectedExercise = if (customExerciseName.text.isNotEmpty()) {
                customExerciseName.text.toString()
            } else {
                // Fallback to the first item in the list if none is selected
                exerciseNames.firstOrNull() ?: ""
            }

            exerciseAdapter.addExerciseRow(selectedExercise)
        }

        builder.setNegativeButton("Cancel", null)
        builder.show()
    }

    // Show pop-up dialog for deleting an exercise row from the table
    private fun showDeleteExerciseDialog() {
        val deleteRowFragment = DeleteRowFragment()
        deleteRowFragment.show(supportFragmentManager, "delete_row")
    }

    // Function to export table data to Excel
    private fun exportDataToExcel() {
        val date = dateTextView.text.toString()
        val exercises = exerciseAdapter.getExercises()

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
