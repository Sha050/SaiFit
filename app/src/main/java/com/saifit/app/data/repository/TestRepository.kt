package com.saifit.app.data.repository

import com.saifit.app.data.model.FitnessTest
import com.saifit.app.data.model.TestCategory

class TestRepository {

    fun getAllTests(): List<FitnessTest> = saiTests

    fun getTestById(id: String): FitnessTest? = saiTests.find { it.id == id }

    fun getTestsByCategory(category: TestCategory): List<FitnessTest> =
        saiTests.filter { it.category == category }

    companion object {
        private val saiTests = listOf(

            FitnessTest(
                id = "height",
                name = "Height Measurement",
                description = "Measures standing height of the athlete in centimeters using a stadiometer.",
                instructions = listOf(
                    "Stand barefoot on a flat surface against the stadiometer.",
                    "Keep heels together, back straight, and arms at side.",
                    "Look straight ahead (Frankfort plane).",
                    "Admin records the reading at the top of the head."
                ),
                unit = "cm",
                category = TestCategory.ANTHROPOMETRIC,
                iconName = "height"
            ),
            FitnessTest(
                id = "weight",
                name = "Body Weight",
                description = "Measures body weight of the athlete in kilograms using a calibrated weighing scale.",
                instructions = listOf(
                    "Wear minimal clothing and remove shoes.",
                    "Stand still on the centre of the weighing scale.",
                    "Keep both feet flat on the platform.",
                    "Admin records the stable reading."
                ),
                unit = "kg",
                category = TestCategory.ANTHROPOMETRIC,
                iconName = "monitor_weight"
            ),

            FitnessTest(
                id = "situps",
                name = "Sit-ups",
                description = "Measures abdominal muscular endurance. Maximum sit-ups performed in 60 seconds.",
                instructions = listOf(
                    "Lie on your back with knees bent at 90°, feet flat on the ground.",
                    "Cross arms over your chest, hands on opposite shoulders.",
                    "A partner holds your feet down.",
                    "On 'Go', curl up until elbows touch mid-thighs, then return.",
                    "Count the total number of complete reps in 60 seconds."
                ),
                unit = "reps",
                category = TestCategory.STRENGTH,
                iconName = "fitness_center"
            ),

            FitnessTest(
                id = "vertical_jump",
                name = "Vertical Jump",
                description = "Measures explosive leg power. The difference between standing reach and jump reach.",
                instructions = listOf(
                    "Stand sideways next to a wall, arm fully extended upward.",
                    "Mark your standing reach height.",
                    "Jump as high as possible and touch the wall at the peak.",
                    "The vertical jump is the difference between the two marks.",
                    "Best of three attempts is recorded."
                ),
                unit = "cm",
                category = TestCategory.POWER,
                iconName = "arrow_upward"
            ),

            FitnessTest(
                id = "shuttle_run",
                name = "Shuttle Run (10 × 4m)",
                description = "Measures agility and speed. The athlete runs back and forth between two lines 4 metres apart, 10 times.",
                instructions = listOf(
                    "Two parallel lines are marked 4 metres apart.",
                    "Start behind Line A.",
                    "On 'Go', sprint to Line B, touch the line, sprint back.",
                    "Complete 10 shuttles (5 round trips) as fast as possible.",
                    "Time is recorded to the nearest 0.1 second."
                ),
                unit = "seconds",
                category = TestCategory.AGILITY,
                iconName = "directions_run"
            ),

            FitnessTest(
                id = "pushups",
                name = "Push-ups",
                description = "Measures upper body strength and endurance. Perform continuous push-ups.",
                instructions = listOf(
                    "Start in a high plank position with hands slightly wider than shoulder-width.",
                    "Keep your body in a straight line from head to heels.",
                    "Lower your body until your chest is close to the floor (elbows at 90 degrees).",
                    "Push back up to the starting position.",
                    "Perform as many repetitions as possible."
                ),
                unit = "reps",
                category = TestCategory.STRENGTH,
                iconName = "fitness_center"
            ),
            FitnessTest(
                id = "squats",
                name = "Bodyweight Squats",
                description = "Measures lower body strength and muscular endurance. Perform continuous squats.",
                instructions = listOf(
                    "Stand with feet shoulder-width apart, toes pointing slightly outward.",
                    "Keep your chest up and back straight.",
                    "Lower your hips back and down as if sitting in a chair, until thighs are parallel to the floor.",
                    "Push through your heels to return to the starting position.",
                    "Perform as many repetitions as possible."
                ),
                unit = "reps",
                category = TestCategory.ENDURANCE,
                iconName = "fitness_center"
            )
        )
    }
}
