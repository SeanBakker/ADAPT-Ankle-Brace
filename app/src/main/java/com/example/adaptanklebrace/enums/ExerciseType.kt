package com.example.adaptanklebrace.enums

import com.example.adaptanklebrace.data.Exercise
import com.example.adaptanklebrace.data.ExerciseInfo

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
        1. Sit comfortably and extend your leg straight in front of you.
        2. Slowly move your foot in circles for 5 seconds.
        3. Complete the rotations repeatedly in the same direction (either clockwise or counterclockwise).
        4. The app will record live data once the exercise is started, until the timer is complete.
        """.trimIndent(),
        5
    ),
    GAIT_TEST(
        "Gait Test",
        "An exercise to monitor improvements in the recovery of gait and impact force of the ankle joint.",
        """
        1. Stand with your feet hip-width apart.
        2. Walk normally in a straight line for 10 seconds.
        3. The app will record live data once the exercise is started, until the timer is complete.
        """.trimIndent(),
        6
    ),
    ERROR(
    "ERROR",
    "Exercise data is unable to be retrieved for the selected exercise. Therefore, we are unable to connect to the device and provide automatic data collection for this exercise.",
        """
        1. You can still perform this exercise manually.
        2. Wear the A.D.A.P.T. device (the device can be left off)
        3. Set the tension level manually to the specified tension level for your exercise goal.
        4. Perform the specified number of reps.
        5. Then repeat for the specified number of sets.
        """.trimIndent(),
    0
    );

    companion object {
        fun getAllExerciseNames(): List<String> {
            return values()
                .filter { it.exerciseName != ERROR.exerciseName && it.exerciseName !in listOf(RANGE_OF_MOTION.exerciseName, GAIT_TEST.exerciseName) }
                .map { it.exerciseName }
        }

        fun getAllMetricNames(): List<String> {
            return values()
                .filter { it.exerciseName != ERROR.exerciseName && it.exerciseName in listOf(RANGE_OF_MOTION.exerciseName, GAIT_TEST.exerciseName) }
                .map { it.exerciseName }
        }

        // Convert the enum values to a list of Exercise objects
        fun getAllExercises(): List<ExerciseInfo> = values()
            .filter { it.exerciseName != ERROR.exerciseName }
            .map {
                ExerciseInfo(
                    name = it.exerciseName,
                    description = it.description,
                    steps = it.steps,
                    imageId = it.imageId
                )
            }

        fun getExerciseInfoByName(name: String): ExerciseInfo? = values()
            .firstOrNull { it.exerciseName == name }
            ?.let {
                ExerciseInfo(
                    name = it.exerciseName,
                    description = it.description,
                    steps = it.steps,
                    imageId = it.imageId
                )
            }

        fun getErrorExerciseInfo(exerciseName: String): ExerciseInfo {
            return ExerciseInfo(
                name = exerciseName,
                description = ERROR.description,
                steps = ERROR.steps,
                imageId = ERROR.imageId
            )
        }

        fun getErrorExercise(): Exercise {
            return Exercise(
                id = -1,
                name = ERROR.exerciseName,
            )
        }

        fun getSize(): Int = values().size
    }
}
