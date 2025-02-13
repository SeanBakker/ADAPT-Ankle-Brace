package com.example.adaptanklebrace.fragments

import android.content.Context
import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.DialogFragment
import com.example.adaptanklebrace.R
import com.example.adaptanklebrace.StartSetActivity
import com.example.adaptanklebrace.utils.GeneralUtil

class AddDifficultyAndCommentsFragment(
    private val context: Context,
    private val expectedTension: Int
) : DialogFragment() {

    private var isManuallyDismissed: Boolean = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_add_difficulty_and_comments, container, false)

        // References to fields
        val tensionLevelInput: EditText = view.findViewById(R.id.tensionLevelInput)
        tensionLevelInput.setText(expectedTension.toString())
        val difficultyLevelInput: EditText = view.findViewById(R.id.difficultyLevelInput)
        val commentsInput: EditText = view.findViewById(R.id.commentsInput)
        val saveDataButton: Button = view.findViewById(R.id.saveDataBtn)

        // Handle button click
        saveDataButton.setOnClickListener {
            val tensionLevel = tensionLevelInput.text.toString().toIntOrNull()
            val difficultyLevel = difficultyLevelInput.text.toString().toIntOrNull()
            val comments = commentsInput.text.toString()

            if (tensionLevel != null && tensionLevel !in 1..10) {
                GeneralUtil.showToast(context, layoutInflater, "Please enter the tension level between 1 and 10 that is set on the device.")
            } else if (difficultyLevel != null && difficultyLevel !in 0..10) {
                GeneralUtil.showToast(context, layoutInflater, "Please enter a difficulty level between 1 and 10, or leave it blank.")
            } else {
                // Save the data
                val startSetActivity = activity as? StartSetActivity
                startSetActivity?.saveSetData(
                    tension = tensionLevel ?: 1,
                    difficulty = difficultyLevel ?: 0,
                    comments = comments
                )
                isManuallyDismissed = true
                dismiss() // Close the dialog
            }
        }

        return view
    }

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)

        if (!isManuallyDismissed) {
            // Save the data even if dialog closes
            val startSetActivity = activity as? StartSetActivity
            startSetActivity?.saveSetData()
        }
    }
}
