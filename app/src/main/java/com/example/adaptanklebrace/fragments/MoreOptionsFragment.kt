package com.example.adaptanklebrace.fragments

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.fragment.app.DialogFragment
import com.example.adaptanklebrace.CommonExercisesActivity
import com.example.adaptanklebrace.NotificationsActivity
import com.example.adaptanklebrace.R
import com.example.adaptanklebrace.SettingsActivity

class MoreOptionsFragment : DialogFragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_more_options, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        view.findViewById<Button>(R.id.btn_settings).setOnClickListener {
            startActivity(Intent(activity, SettingsActivity::class.java))
            dismiss()
        }

        view.findViewById<Button>(R.id.btn_common_exercises).setOnClickListener {
            startActivity(Intent(activity, CommonExercisesActivity::class.java))
            dismiss()
        }

        view.findViewById<Button>(R.id.btn_notifications).setOnClickListener {
            startActivity(Intent(activity, NotificationsActivity::class.java))
            dismiss()
        }
    }
}
