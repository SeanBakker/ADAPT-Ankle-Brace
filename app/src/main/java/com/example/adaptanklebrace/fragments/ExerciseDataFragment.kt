package com.example.adaptanklebrace.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.adaptanklebrace.data.Exercise
import com.example.adaptanklebrace.databinding.FragmentItemExerciseDataBinding

class ExerciseDataFragment : Fragment() {

    private var _binding: FragmentItemExerciseDataBinding? = null
    private val binding get() = _binding!!

    // Create a new instance of ExerciseDataFragment and pass in the exercise data as arguments
    companion object {
        private const val ARG_EXERCISE = "exerciseData"

        fun newInstance(exercise: Exercise): ExerciseDataFragment {
            val fragment = ExerciseDataFragment()
            val bundle = Bundle()
            bundle.putSerializable(ARG_EXERCISE, exercise) // Pass the exercise data object
            fragment.arguments = bundle
            return fragment
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentItemExerciseDataBinding.inflate(inflater, container, false)

        // Retrieve the exercise data passed through arguments
        @Suppress("DEPRECATION")
        val exercise = arguments?.getSerializable(ARG_EXERCISE) as? Exercise

        // Bind the exercise data to the layout
        binding.exerciseData = exercise

        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
