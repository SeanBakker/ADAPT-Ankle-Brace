package com.example.adaptanklebrace.enums

import android.os.Build
import androidx.annotation.RequiresApi
import com.example.adaptanklebrace.data.Exercise

enum class ExerciseType(val exerciseName: String, val description: String, val steps: String, val imageId: Int) {
    PLANTAR_FLEXION(
        "Plantar Flexion",
        "A stretch that targets the calf and foot muscles to improve flexibility and range of motion.",
        """
        1. Sit down with your legs extended straight in front of you.
        2. Flex your foot downward, pointing your toes as far as possible.
        3. Hold the stretch for a specified duration and repeat according to the number of reps.
        4. Repeat for the specified number of sets.
        """.trimIndent(),
        1
    ),
    DORSIFLEXION(
        "Dorsiflexion",
        "This exercise improves ankle flexibility and strengthens the muscles on the front of the lower leg.",
        """
        1. Sit down with your legs extended straight in front of you.
        2. Flex your foot upward, bringing your toes toward your shin.
        3. Hold the stretch for a specified duration and repeat according to the number of reps.
        4. Repeat for the specified number of sets.
        """.trimIndent(),
        2
    ),
    INVERSION(
        "Inversion",
        "An exercise that targets the ankle's ability to move inward, strengthening the muscles that stabilize the ankle.",
        """
        1. Sit down with your legs extended straight in front of you.
        2. Slowly rotate your foot inward, working the muscles on the inside of your ankle.
        3. Hold the stretch for a specified duration and repeat according to the number of reps.
        4. Repeat for the specified number of sets.
        """.trimIndent(),
        3
    ),
    EVERSION(
        "Eversion",
        "Strengthens the muscles on the outside of the ankle to improve lateral stability and flexibility.",
        """
        1. Sit down with your legs extended straight in front of you.
        2. Slowly rotate your foot outward, working the muscles on the outside of your ankle.
        3. Hold the stretch for a specified duration and repeat according to the number of reps.
        4. Repeat for the specified number of sets.
        """.trimIndent(),
        4
    ),
    RANGE_OF_MOTION(
        "Range of Motion (ROM)",
        "An exercise to monitor improvements in flexibility of the ankle joint.",
        """
        1. Sit comfortably and extend one leg straight in front of you.
        2. Slowly move your foot in circles, first in the clockwise direction.
        3. Perform the specified number of reps (rotations).
        4. After a short break, repeat the exercise in the counterclockwise direction.
        """.trimIndent(),
        5
    ),
    GAIT_SYMMETRY(
        "Gait Symmetry",
        "An exercise to monitor improvements in the recovery of gait of the ankle joint.",
        """
        1. Stand with your feet hip-width apart.
        2. Walk in a straight line, with each step of equal length.
        3. Perform the specified number of reps (steps).
        4. After a short break, repeat for the specified number of sets.
        """.trimIndent(),
        6
    ),
    IMPACT_FORCE(
        "Impact Force",
        "An exercise to monitor improvements in the body's ability to absorb shock during the walking cycle.",
        """
        1. Stand with your feet hip-width apart.
        2. Walk in a straight line, with each step of equal length.
        3. Perform the specified number of reps (steps).
        4. After a short break, repeat for the specified number of sets.
        """.trimIndent(),
        7
    );

    companion object {
        // Convert the enum values to a list of Exercise objects
        @RequiresApi(Build.VERSION_CODES.Q)
        fun getAllExercises(): List<Exercise> = values().map {
            Exercise(
                name = it.exerciseName,
                description = it.description,
                steps = it.steps,
                imageId = it.imageId
            )
        }

        fun getAllExerciseNames(): List<String> {
            return values().map { it.exerciseName }
        }

        fun getSize(): Int = values().size
    }
}
