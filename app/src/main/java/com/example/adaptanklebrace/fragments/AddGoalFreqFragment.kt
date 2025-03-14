package com.example.adaptanklebrace.fragments

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.DialogFragment
import com.example.adaptanklebrace.R
import com.example.adaptanklebrace.adapters.RecoveryExerciseAdapter
import com.example.adaptanklebrace.adapters.RecoveryMetricAdapter
import com.example.adaptanklebrace.data.Exercise
import com.example.adaptanklebrace.data.Metric
import com.example.adaptanklebrace.utils.GeneralUtil

class AddGoalFreqFragment(
    private val context: Context,
    private val exerciseAdapter: RecoveryExerciseAdapter? = null,
    private val exercise: Exercise? = null,
    private val metricAdapter: RecoveryMetricAdapter? = null,
    private val metric: Metric? = null,
    private val position: Int
) : DialogFragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_add_goal_freq, container, false)

        // References to fields
        val freqNumberInput: EditText = view.findViewById(R.id.freqNumberInput)
        val freqCategorySpinner: Spinner = view.findViewById(R.id.freqCategorySpinner)
        val setFreqButton: Button = view.findViewById(R.id.setFreqBtn)

        // Handle button click
        setFreqButton.setOnClickListener {
            val freqNumber = freqNumberInput.text.toString().toIntOrNull()
            val freqCategory = freqCategorySpinner.selectedItem?.toString()

            if (freqNumber == null || freqNumber <= 0) {
                GeneralUtil.showToast(context, layoutInflater, getString(R.string.enterValidFrequencyNumberToast))
            } else if (freqCategory.isNullOrEmpty()) {
                GeneralUtil.showToast(context, layoutInflater, getString(R.string.selectFrequencyCategoryToast))
            } else {
                if (exercise != null && exerciseAdapter != null) {
                    // Update the frequency field of the exercise & notify the adapter
                    exercise.frequency = freqNumber.toString() + "x/" + freqCategory
                    exerciseAdapter.notifyItemChangedAndRefresh(position)
                } else if (metric != null && metricAdapter != null) {
                    // Update the frequency field of the exercise & notify the adapter
                    metric.frequency = freqNumber.toString() + "x/" + freqCategory
                    metricAdapter.notifyItemChangedAndRefresh(position)
                }
                dismiss() // Close the dialog
            }
        }

        return view
    }
}
