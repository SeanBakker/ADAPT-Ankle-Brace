package com.example.adaptanklebrace.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.adaptanklebrace.data.Exercise
import com.example.adaptanklebrace.databinding.ItemExerciseBinding

class ExerciseFragment : Fragment() {

    private var _binding: ItemExerciseBinding? = null
    private val binding get() = _binding!!

    // Create a new instance of ExerciseFragment and pass in the exercise data as arguments
    companion object {
        private const val ARG_EXERCISE = "exercise"

        fun newInstance(exercise: Exercise): ExerciseFragment {
            val fragment = ExerciseFragment()
            val bundle = Bundle()
            bundle.putSerializable(ARG_EXERCISE, exercise) // Pass the exercise object
            fragment.arguments = bundle
            return fragment
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = ItemExerciseBinding.inflate(inflater, container, false)

        // Retrieve the exercise passed through arguments
        val exercise = arguments?.getSerializable(ARG_EXERCISE) as? Exercise

        // Bind the exercise data to the layout
        binding.exercise = exercise

        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
