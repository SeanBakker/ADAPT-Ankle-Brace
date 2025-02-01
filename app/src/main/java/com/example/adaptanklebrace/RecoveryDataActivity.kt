package com.example.adaptanklebrace

import android.annotation.SuppressLint
import android.app.DatePickerDialog
import android.content.res.ColorStateList
import android.os.Bundle
import android.view.MotionEvent
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import androidx.core.widget.addTextChangedListener
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.adaptanklebrace.adapters.RecoveryDataTableRowAdapter
import com.example.adaptanklebrace.data.Exercise
import com.example.adaptanklebrace.fragments.AddExerciseDataRowFragment
import com.example.adaptanklebrace.fragments.DeleteRowFragment
import com.example.adaptanklebrace.utils.ExerciseDataStore
import com.example.adaptanklebrace.utils.ExerciseUtil
import com.example.adaptanklebrace.utils.GeneralUtil
import java.io.File
import java.text.SimpleDateFormat
import java.time.LocalTime
import java.util.*

class RecoveryDataActivity : AppCompatActivity(), RecoveryDataTableRowAdapter.RecoveryDataCallback,
    DeleteRowFragment.OnDeleteListener {

    private lateinit var dateTextView: TextView
    private lateinit var datePickerButton: Button
    private lateinit var difficultyProgressBar: ProgressBar
    private lateinit var difficultyTextView: TextView
    private lateinit var commentsEditText: EditText
    private lateinit var exerciseRecyclerView: RecyclerView
    private lateinit var exportButton: Button
    private lateinit var importButton: Button
    private lateinit var addExerciseButton: Button
    private lateinit var deleteExerciseButton: Button

    private lateinit var exerciseAdapter: RecoveryDataTableRowAdapter
    private var exercises: MutableList<Exercise> = mutableListOf()

    companion object {
        const val RECOVERY_DATA_PREFERENCE = "RecoveryData"
    }

    @SuppressLint("ClickableViewAccessibility", "SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_recovery_data)

        // Set up the toolbar
        val toolbar: Toolbar = findViewById(R.id.recoveryDataToolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.title = getString(R.string.recovery_data)

        // Enable the back button
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)

        // Handle the back button click
        toolbar.setNavigationOnClickListener {
            GeneralUtil.returnToMainActivity(this)
            // Apply a smooth transition
            overridePendingTransition(android.R.anim.slide_in_left, android.R.anim.slide_out_right)
            finish() // Close activity
        }

        // Initialize views
        dateTextView = findViewById(R.id.dateText)
        datePickerButton = findViewById(R.id.datePickerButton)
        difficultyProgressBar = findViewById(R.id.difficultyProgressBar)
        difficultyProgressBar.progressTintList = ColorStateList.valueOf(ContextCompat.getColor(this, R.color.red_1))
        difficultyTextView = findViewById(R.id.difficultyText)
        commentsEditText = findViewById(R.id.commentsEditText)
        exerciseRecyclerView = findViewById(R.id.exerciseRecyclerView)
        exportButton = findViewById(R.id.exportButton)
        importButton = findViewById(R.id.importButton)
        addExerciseButton = findViewById(R.id.addExerciseButton)
        deleteExerciseButton = findViewById(R.id.deleteExerciseButton)

        // Initialize the adapter and pass the activity as a callback
        exerciseAdapter = RecoveryDataTableRowAdapter(exercises, this)

        // Set up RecyclerView
        exerciseRecyclerView.layoutManager = LinearLayoutManager(this)
        exerciseRecyclerView.adapter = exerciseAdapter

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

        // Handle add exercise button click
        addExerciseButton.setOnClickListener { showAddExerciseDialog() }

        // Handle delete exercise button click
        deleteExerciseButton.setOnClickListener { showDeleteExerciseDialog() }

        // Load data for today's date on activity start
        val currentDate = getCurrentDate()
        dateTextView.text = currentDate
        loadDateData(currentDate)
    }

    // Save exercise data for the selected date to the apps storage
    override fun saveCurrentDateExerciseData() {
        val date = dateTextView.text.toString()
        val exercises = exerciseAdapter.getExercises()
        ExerciseDataStore(this, RECOVERY_DATA_PREFERENCE).saveExercisesForDate(date, exercises)
    }

    override fun onDeleteRow() {
        deleteExerciseRow()
    }

    fun onAddRow(exercise: Exercise) {
        addExerciseRow(exercise)
    }

    private fun deleteExerciseRow() {
        exerciseAdapter.deleteExerciseRow()
        saveCurrentDateExerciseData() // Save data after deleting rows
    }

    private fun addExerciseRow(exercise: Exercise) {
        exerciseAdapter.addExerciseRow(exercise)
        saveCurrentDateExerciseData() // Save data after adding new row
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

    private fun getCurrentDate(): String {
        val calendar = Calendar.getInstance()
        val selectedDate = calendar.time
        val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        val formattedDate = dateFormat.format(selectedDate)

        return formattedDate
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
    }

    // Save difficulty & comments data for the selected date to the apps storage
    private fun saveDifficultyAndCommentsDateData() {
        val date = dateTextView.text.toString()
        val difficulty = difficultyProgressBar.progress
        val comments = commentsEditText.text.toString()
        ExerciseDataStore(this, RECOVERY_DATA_PREFERENCE).saveDifficultyAndCommentsPairForDate(date, difficulty, comments)
    }

    // Show pop-up dialog for adding exercise data row to the table
    private fun showAddExerciseDialog() {
        val addExerciseDataRowFragment = AddExerciseDataRowFragment(this, exerciseAdapter)
        addExerciseDataRowFragment.show(supportFragmentManager, "add_exercise_data_row")
    }

    // Show pop-up dialog for deleting an exercise row from the table
    private fun showDeleteExerciseDialog() {
        val deleteRowFragment = DeleteRowFragment()
        deleteRowFragment.show(supportFragmentManager, "delete_row")
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
            ExerciseUtil.showToast(this, layoutInflater, "Data exported to $fileName")
        } catch (e: Exception) {
            e.printStackTrace()
            ExerciseUtil.showToast(this, layoutInflater, "Export failed")
        }
    }

    // Function to import table data from Excel
    private fun importDataFromExcel() {
        val date = dateTextView.text.toString()
        try {
            val file = File(getExternalFilesDir(null), "RecoveryData_$date.csv")
            if (!file.exists()) {
                ExerciseUtil.showToast(this, layoutInflater, "No file found to import.")
                return
            }

            val exercises = mutableListOf<Exercise>()
            file.forEachLine { line ->
                val columns = line.split(",")
                if (columns[0] == "Exercise") return@forEachLine // Skip header

                exercises.add(
                    Exercise(
                        id = ExerciseUtil.generateNewId(exercises),
                        name = columns[0],
                        sets = columns[1].toInt(),
                        reps = columns[2].toInt(),
                        hold = columns[3].toInt(),
                        tension = columns[4].toInt(),
                        timeCompleted = LocalTime.parse(columns[5], ExerciseUtil.timeFormatter),
                        difficulty = columns[6].toInt(),
                        comments = columns[7],
                        isSelected = columns[8].toBoolean()
                    )
                )
            }

            exerciseAdapter.setExercises(exercises)
            ExerciseUtil.showToast(this, layoutInflater, "Data imported successfully.")
        } catch (e: Exception) {
            e.printStackTrace()
            ExerciseUtil.showToast(this, layoutInflater, "Import failed")
        }
    }
}
