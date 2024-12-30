package com.example.adaptanklebrace.enums

import com.example.adaptanklebrace.data.Exercise

enum class ExerciseType(val exerciseName: String, val description: String, val steps: String, val imageId: Int) {
    ANKLE_STRETCH("Ankle Stretch", "Description of ankle stretch", "Steps to do it", 1),
    HEEL_RAISE("Heel Raise", "Description of heel raise", "Steps to do it", 2);

    companion object {
        // Convert the enum values to a list of Exercise objects
        fun getAllExercises(): List<Exercise> = values().map {
            Exercise(
                name = it.exerciseName,
                description = it.description,
                steps = it.steps,
                imageId = it.imageId
            )
        }

        fun getSize(): Int = values().size
    }
}

