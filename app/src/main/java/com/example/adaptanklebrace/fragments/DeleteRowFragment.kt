package com.example.adaptanklebrace.fragments

import android.content.Context
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.annotation.RequiresApi
import androidx.fragment.app.DialogFragment
import com.example.adaptanklebrace.R

class DeleteRowFragment : DialogFragment() {

    // Define an interface to communicate with the activity
    interface OnDeleteListener {
        fun onDeleteRow()
    }

    private var listener: OnDeleteListener? = null

    @RequiresApi(Build.VERSION_CODES.S)
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.delete_exercise_row_fragment, container, false)

        val deleteDataButton: Button = view.findViewById(R.id.deleteDataBtn)
        deleteDataButton.setOnClickListener {
            deleteData()
        }

        return view
    }

    @RequiresApi(Build.VERSION_CODES.S)
    private fun deleteData() {
        // Call back to the activity through the interface
        listener?.onDeleteRow()
        dismiss() // Close the dialog
    }

    // Ensure the activity implements the interface
    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is OnDeleteListener) {
            listener = context
        } else {
            throw RuntimeException("$context must implement OnDeleteListener")
        }
    }

    override fun onDetach() {
        super.onDetach()
        listener = null // Avoid memory leaks
    }
}
