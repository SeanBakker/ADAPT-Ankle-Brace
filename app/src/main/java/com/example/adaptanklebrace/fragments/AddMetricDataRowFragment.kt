package com.example.adaptanklebrace.fragments

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.DialogFragment
import com.example.adaptanklebrace.R
import com.example.adaptanklebrace.RecoveryDataActivity
import com.example.adaptanklebrace.adapters.RecoveryMetricAdapter
import com.example.adaptanklebrace.data.Metric
import com.example.adaptanklebrace.enums.ExerciseType
import com.example.adaptanklebrace.utils.ExerciseUtil
import com.example.adaptanklebrace.utils.GeneralUtil
import java.time.LocalTime
import java.util.Calendar

class AddMetricDataRowFragment(
    private val context: Context,
    private val metricAdapter: RecoveryMetricAdapter
) : DialogFragment() {

    private lateinit var timeInput: TextView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_add_metric_data_row, container, false)

        // References to fields
        val metricNameDropdown: AutoCompleteTextView = view.findViewById(R.id.metricNameDropdown)
        timeInput = view.findViewById(R.id.timeInput)
        val difficultyLevelInput: EditText = view.findViewById(R.id.difficultyLevelInput)
        val commentsInput: EditText = view.findViewById(R.id.commentsInput)
        val addMetricDataButton: Button = view.findViewById(R.id.addMetricDataBtn)

        // Initialize metric name dropdown
        val metricNames = ExerciseType.getAllMetricNames()
        val adapter = ArrayAdapter(context, android.R.layout.simple_dropdown_item_1line, metricNames)
        metricNameDropdown.setAdapter(adapter)

        // Show all suggestions when the field gains focus
        metricNameDropdown.setOnClickListener {
            metricNameDropdown.showDropDown()
        }

        // Set default time to the current time
        GeneralUtil.updateTimeInput(timeInput, Calendar.getInstance())

        // Handle time input click
        timeInput.setOnClickListener {
            GeneralUtil.showTimePickerDialog(context, timeInput)
        }

        // Handle button click
        addMetricDataButton.setOnClickListener {
            val metricName = metricNameDropdown.text.toString().trim()
            val timeOfCompletion = timeInput.text.toString()
            val difficultyLevel = difficultyLevelInput.text.toString().toIntOrNull()
            val comments = commentsInput.text.toString()

            //todo: add option to pick dates for which the metric was completed for

            if (metricName.isEmpty()) {
                GeneralUtil.showToast(context, layoutInflater, getString(R.string.enterMetricNameToast))
            } else if (difficultyLevel != null && difficultyLevel !in 1..10) {
                GeneralUtil.showToast(context, layoutInflater, getString(R.string.enterDifficultyLevelToast))
            } else {
                val metrics = metricAdapter.getMetrics()

                // Create the metric object
                val metric = Metric(
                    id = ExerciseUtil.generateNewMetricId(metrics),
                    name = metricName,
                    timeCompleted = LocalTime.parse(timeOfCompletion, GeneralUtil.timeFormatter),
                    difficulty = difficultyLevel ?: 0,
                    comments = comments,
                    isManuallyRecorded = true
                )

                // Add the metric to RecoveryDataActivity
                val recoveryDataActivity = activity as? RecoveryDataActivity
                recoveryDataActivity?.onAddRow(metric)
                dismiss() // Close the dialog
            }
        }

        return view
    }
}
