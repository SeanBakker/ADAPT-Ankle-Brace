package com.example.adaptanklebrace.fragments

import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.annotation.RequiresApi
import androidx.fragment.app.DialogFragment
import com.example.adaptanklebrace.R
import com.example.adaptanklebrace.RecoveryPlanActivity

class DeleteRowFragment : DialogFragment() {

    @RequiresApi(Build.VERSION_CODES.S)
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.delete_row_fragment, container, false)

        val deleteDataButton: Button = view.findViewById(R.id.deleteDataBtn)
        deleteDataButton.setOnClickListener {
            deleteData()
        }

        return view
    }

    @RequiresApi(Build.VERSION_CODES.S)
    private fun deleteData() {
        // Get reference to RecoveryPlanActivity
        val recoveryPlanActivity = activity as? RecoveryPlanActivity

        recoveryPlanActivity?.let {
            recoveryPlanActivity.deleteExerciseRow()
            dismiss() // Close the dialog
        }
    }
}
