package com.example.adaptanklebrace.fragments

import android.content.Context
import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.DialogFragment
import com.example.adaptanklebrace.GaitMetricActivity
import com.example.adaptanklebrace.R
import com.example.adaptanklebrace.ROMMetricActivity
import com.example.adaptanklebrace.StartSetActivity
import com.example.adaptanklebrace.utils.GeneralUtil

class AddDifficultyAndCommentsFragment(
    private val context: Context,
    private val expectedTension: Int = 1,
    private val isROMTest: Boolean = false,
    private val isGaitTest: Boolean = false
) : DialogFragment() {

    private var isManuallyDismissed: Boolean = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_add_difficulty_and_comments, container, false)

        // References to fields
        val tensionLevelInput: EditText = view.findViewById(R.id.tensionLevelInput)
        val tensionLevelInputText: LinearLayout = view.findViewById(R.id.tensionLevelInputText)
        val difficultyLevelInput: EditText = view.findViewById(R.id.difficultyLevelInput)
        val commentsInput: EditText = view.findViewById(R.id.commentsInput)
        val saveDataButton: Button = view.findViewById(R.id.saveDataBtn)

        // Show/hide tension level for exercises/metrics
        if (isROMTest || isGaitTest) {
            tensionLevelInput.visibility = View.GONE
            tensionLevelInputText.visibility = View.GONE
        } else {
            tensionLevelInput.setText(expectedTension.toString())
        }

        // Handle button click
        saveDataButton.setOnClickListener {
            val tensionLevel = tensionLevelInput.text.toString().toIntOrNull()
            val difficultyLevel = difficultyLevelInput.text.toString().toIntOrNull()
            val comments = commentsInput.text.toString()

            if (tensionLevel != null && tensionLevel !in 1..10) {
                GeneralUtil.showToast(context, layoutInflater, getString(R.string.enterTensionOnDeviceToast))
            } else if (difficultyLevel != null && difficultyLevel !in 0..10) {
                GeneralUtil.showToast(context, layoutInflater, getString(R.string.enterDifficultyLevelToast))
            } else {
                if (isROMTest) {
                    // Save the metric data
                    val romMetricActivity = activity as? ROMMetricActivity
                    romMetricActivity?.saveROMMetricData(
                        difficulty = difficultyLevel ?: 0,
                        comments = comments
                    )
                } else if (isGaitTest) {
                    // Save the metric data
                    val gaitMetricActivity = activity as? GaitMetricActivity
                    gaitMetricActivity?.saveGaitMetricData(
                        difficulty = difficultyLevel ?: 0,
                        comments = comments
                    )
                } else {
                    // Save the exercise data
                    val startSetActivity = activity as? StartSetActivity
                    startSetActivity?.saveSetData(
                        tension = tensionLevel ?: 1,
                        difficulty = difficultyLevel ?: 0,
                        comments = comments
                    )
                }
                isManuallyDismissed = true
                dismiss() // Close the dialog
            }
        }

        return view
    }

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)

        // Save the data even if dialog closes
        if (!isManuallyDismissed) {
            if (isROMTest) {
                // Save ROM metric data
                val romMetricActivity = activity as? ROMMetricActivity
                romMetricActivity?.saveROMMetricData()
            } else if (isGaitTest) {
                // Save Gait metric data
                val gaitMetricActivity = activity as? GaitMetricActivity
                gaitMetricActivity?.saveGaitMetricData()
            } else {
                // Save exercise set data
                val startSetActivity = activity as? StartSetActivity
                startSetActivity?.saveSetData()
            }
        }
    }
}
