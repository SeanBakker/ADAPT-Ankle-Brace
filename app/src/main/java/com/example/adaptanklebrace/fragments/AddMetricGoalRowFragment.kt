package com.example.adaptanklebrace.fragments

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import androidx.fragment.app.DialogFragment
import com.example.adaptanklebrace.R
import com.example.adaptanklebrace.RecoveryPlanActivity
import com.example.adaptanklebrace.adapters.RecoveryMetricAdapter
import com.example.adaptanklebrace.data.Metric
import com.example.adaptanklebrace.enums.ExerciseType
import com.example.adaptanklebrace.utils.ExerciseUtil
import com.example.adaptanklebrace.utils.GeneralUtil

class AddMetricGoalRowFragment(
    private val context: Context,
    private val metricAdapter: RecoveryMetricAdapter
) : DialogFragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_add_metric_goal_row, container, false)

        // References to fields
        val metricNameDropdown: AutoCompleteTextView = view.findViewById(R.id.metricNameDropdown)
        val freqNumberInput: EditText = view.findViewById(R.id.freqNumberInput)
        val freqCategorySpinner: Spinner = view.findViewById(R.id.freqCategorySpinner)
        val commentsInput: EditText = view.findViewById(R.id.commentsInput)
        val addMetricGoalButton: Button = view.findViewById(R.id.addMetricGoalBtn)

        // Initialize metric name dropdown
        val metricNames = ExerciseType.getAllMetricNames()
        val adapter = ArrayAdapter(context, android.R.layout.simple_dropdown_item_1line, metricNames)
        metricNameDropdown.setAdapter(adapter)

        // Show all suggestions when the field is clicked
        metricNameDropdown.setOnClickListener {
            metricNameDropdown.showDropDown()
        }

        // Handle button click
        addMetricGoalButton.setOnClickListener {
            val metricName = metricNameDropdown.text.toString().trim()
            val freqNumber = freqNumberInput.text.toString().toIntOrNull()
            val freqCategory = freqCategorySpinner.selectedItem?.toString()
            val comments = commentsInput.text.toString()

            val recoveryPlanActivity = activity as? RecoveryPlanActivity

            // Get the list of metric goals currently in the table
            val metricGoals = recoveryPlanActivity?.getMetricGoals(metricAdapter) ?: listOf()

            //todo: add option to add goal for current week or all future weeks or choose weeks

            if (metricName.isEmpty()) {
                GeneralUtil.showToast(context, layoutInflater, "Please enter a metric name.")
            } else if (metricGoals.any { metric: Metric ->
                    metric.name.equals(metricName, ignoreCase = true)
                }) {
                GeneralUtil.showToast(context, layoutInflater, "This metric already exists in the table, please edit the goal instead.")
            } else if (freqNumber == null || freqNumber <= 0) {
                GeneralUtil.showToast(context, layoutInflater, "Please enter a valid frequency number.")
            } else if (freqCategory.isNullOrEmpty()) {
                GeneralUtil.showToast(context, layoutInflater, "Please select a frequency category.")
            } else {
                // Create the metric object
                val metric = Metric(
                    id = ExerciseUtil.generateNewMetricId(metricGoals),
                    name = metricName,
                    frequency = freqNumber.toString() + "x/" + freqCategory,
                    comments = comments
                )

                // Call the RecoveryPlanActivity function to add the goal
                recoveryPlanActivity?.onAddMetricRow(metric)
                dismiss() // Close the dialog
            }
        }

        return view
    }
}
