package com.example.adaptanklebrace.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.adaptanklebrace.data.ExerciseInfo
import com.example.adaptanklebrace.databinding.FragmentItemExerciseInfoBinding
import com.example.adaptanklebrace.enums.ExerciseType

class ExerciseInfoFragment : Fragment() {

    private var _binding: FragmentItemExerciseInfoBinding? = null
    private val binding get() = _binding!!

    companion object {
        private const val ARG_EXERCISE = "exerciseInfo"

        fun newInstance(exerciseInfo: ExerciseInfo): ExerciseInfoFragment {
            return ExerciseInfoFragment().apply {
                arguments = Bundle().apply {
                    putSerializable(ARG_EXERCISE, exerciseInfo) // Pass the exercise info object
                }
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentItemExerciseInfoBinding.inflate(inflater, container, false)

        // Retrieve the exercise info passed through arguments
        @Suppress("DEPRECATION")
        val exercise = arguments?.getSerializable(ARG_EXERCISE) as? ExerciseInfo

        // Bind the exercise info to the layout
        binding.exerciseInfo = exercise

        // Manage VideoView visibility based on exercise validity
        if (exercise?.name == ExerciseType.ERROR.exerciseName) {
            binding.exerciseVideo.visibility = View.GONE
        } else {
            binding.exerciseVideo.visibility = View.VISIBLE
        }

        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
