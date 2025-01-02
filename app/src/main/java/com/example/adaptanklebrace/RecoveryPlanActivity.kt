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
import com.example.adaptanklebrace.adapters.ExerciseTableRowAdapter
import com.example.adaptanklebrace.data.Exercise
import com.example.adaptanklebrace.enums.ExerciseType
import com.example.adaptanklebrace.fragments.DeleteRowFragment
import java.io.File
import java.util.*

class RecoveryPlanActivity : AppCompatActivity(), ExerciseTableRowAdapter.SaveDataCallback {

    private lateinit var dateTextView: TextView
    private lateinit var datePickerButton: Button
    private lateinit var difficultyProgressBar: ProgressBar
    private lateinit var commentsEditText: EditText
    private lateinit var exerciseRecyclerView: RecyclerView
    private lateinit var exportButton: Button
    private lateinit var importButton: Button
    private lateinit var addExerciseButton: Button
    private lateinit var deleteExerciseButton: Button

    lateinit var exerciseAdapter: ExerciseTableRowAdapter
    private var exercises: MutableList<Exercise> = mutableListOf()

    @RequiresApi(Build.VERSION_CODES.Q)
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
        exerciseAdapter = ExerciseTableRowAdapter(this, exercises, this)

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

        // Load data for today's date on activity start
        val currentDate = getCurrentDate()
        dateTextView.text = currentDate
        loadDateData(currentDate)
    }

    override fun saveCurrentDateData() {
        val date = dateTextView.text.toString()
        val exercises = exerciseAdapter.getExercises()
        ExerciseDataStore(this).saveExercisesForDate(date, exercises)
    }

    fun deleteExerciseRow() {
        exerciseAdapter.deleteExerciseRow()
        saveCurrentDateData() // Save data after deleting rows
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun addExerciseRow() {
        exerciseAdapter.addExerciseRow("test")
        saveCurrentDateData() // Save data after adding new row
    }

    private fun showDatePicker() {
        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)

        val datePickerDialog = DatePickerDialog(
            this,
            { _, selectedYear, selectedMonth, selectedDay ->
                val selectedDate = "$selectedDay/${selectedMonth + 1}/$selectedYear"
                dateTextView.text = selectedDate

                // Load data for the selected date
                loadDateData(selectedDate)
            },
            year,
            month,
            day
        )
        datePickerDialog.show()
    }

    private fun getCurrentDate(): String {
        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH) + 1
        val day = calendar.get(Calendar.DAY_OF_MONTH)
        return "$day/$month/$year"
    }

    private fun loadDateData(date: String) {
        val exercises = ExerciseDataStore(this).getExercisesForDate(date)
        exerciseAdapter.setExercises(exercises)
    }

    // Show pop-up dialog for choosing exercise type to add to the table
    @RequiresApi(Build.VERSION_CODES.Q)
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
                        difficulty = columns[6].toInt(),
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
