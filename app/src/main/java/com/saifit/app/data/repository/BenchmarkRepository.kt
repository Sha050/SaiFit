package com.saifit.app.data.repository

import com.saifit.app.data.model.*

class BenchmarkRepository {

    fun evaluate(
        testId: String,
        testName: String,
        value: Double,
        unit: String,
        age: Int,
        gender: Gender
    ): BenchmarkComparison? {
        val benchmark = findBenchmark(testId, age, gender) ?: return null
        val lowerIsBetter = testId in lowerIsBetterTests

        val tier = computeTier(value, benchmark, lowerIsBetter)
        val percentile = computePercentile(value, benchmark, lowerIsBetter)

        return BenchmarkComparison(
            testId = testId,
            testName = testName,
            athleteValue = value,
            unit = unit,
            tier = tier,
            percentile = percentile,
            benchmark = benchmark,
            lowerIsBetter = lowerIsBetter
        )
    }

    private fun findBenchmark(testId: String, age: Int, gender: Gender): Benchmark? =
        benchmarks.find { it.testId == testId && it.gender == gender && age in it.ageMin..it.ageMax }

    private fun computeTier(value: Double, b: Benchmark, lowerIsBetter: Boolean): PerformanceTier {
        return if (lowerIsBetter) {

            when {
                value <= b.elite     -> PerformanceTier.ELITE
                value <= b.excellent -> PerformanceTier.EXCELLENT
                value <= b.good      -> PerformanceTier.GOOD
                value <= b.average   -> PerformanceTier.AVERAGE
                else                 -> PerformanceTier.BELOW_AVERAGE
            }
        } else {

            when {
                value >= b.elite     -> PerformanceTier.ELITE
                value >= b.excellent -> PerformanceTier.EXCELLENT
                value >= b.good      -> PerformanceTier.GOOD
                value >= b.average   -> PerformanceTier.AVERAGE
                else                 -> PerformanceTier.BELOW_AVERAGE
            }
        }
    }

    private fun computePercentile(value: Double, b: Benchmark, lowerIsBetter: Boolean): Int {

        val range = if (lowerIsBetter) {

            when {
                value <= b.elite     -> 95
                value <= b.excellent -> 75 + ((b.excellent - value) / (b.excellent - b.elite) * 20).toInt().coerceIn(0, 20)
                value <= b.good      -> 55 + ((b.good - value) / (b.good - b.excellent) * 20).toInt().coerceIn(0, 20)
                value <= b.average   -> 35 + ((b.average - value) / (b.average - b.good) * 20).toInt().coerceIn(0, 20)
                value <= b.poor      -> 15 + ((b.poor - value) / (b.poor - b.average) * 20).toInt().coerceIn(0, 20)
                else                 -> 5
            }
        } else {
            when {
                value >= b.elite     -> 95
                value >= b.excellent -> 75 + ((value - b.excellent) / (b.elite - b.excellent) * 20).toInt().coerceIn(0, 20)
                value >= b.good      -> 55 + ((value - b.good) / (b.excellent - b.good) * 20).toInt().coerceIn(0, 20)
                value >= b.average   -> 35 + ((value - b.average) / (b.good - b.average) * 20).toInt().coerceIn(0, 20)
                value >= b.poor      -> 15 + ((value - b.poor) / (b.average - b.poor) * 20).toInt().coerceIn(0, 20)
                else                 -> 5
            }
        }
        return range.coerceIn(1, 99)
    }

    companion object {

        private val lowerIsBetterTests = setOf("shuttle_run", "endurance_run_800m", "endurance_run_1600m")

        private val benchmarks = listOf(

            Benchmark("height", Gender.MALE, 14, 18, poor = 150.0, average = 160.0, good = 168.0, excellent = 175.0, elite = 180.0),
            Benchmark("height", Gender.FEMALE, 14, 18, poor = 145.0, average = 153.0, good = 160.0, excellent = 167.0, elite = 172.0),
            Benchmark("height", Gender.MALE, 19, 25, poor = 155.0, average = 165.0, good = 172.0, excellent = 178.0, elite = 183.0),
            Benchmark("height", Gender.FEMALE, 19, 25, poor = 148.0, average = 156.0, good = 163.0, excellent = 169.0, elite = 174.0),

            Benchmark("weight", Gender.MALE, 14, 18, poor = 40.0, average = 50.0, good = 58.0, excellent = 65.0, elite = 72.0),
            Benchmark("weight", Gender.FEMALE, 14, 18, poor = 35.0, average = 45.0, good = 52.0, excellent = 58.0, elite = 64.0),
            Benchmark("weight", Gender.MALE, 19, 25, poor = 50.0, average = 58.0, good = 65.0, excellent = 72.0, elite = 80.0),
            Benchmark("weight", Gender.FEMALE, 19, 25, poor = 40.0, average = 48.0, good = 55.0, excellent = 62.0, elite = 68.0),

            Benchmark("situps", Gender.MALE, 14, 18, poor = 15.0, average = 25.0, good = 33.0, excellent = 40.0, elite = 48.0),
            Benchmark("situps", Gender.FEMALE, 14, 18, poor = 10.0, average = 20.0, good = 28.0, excellent = 35.0, elite = 42.0),
            Benchmark("situps", Gender.MALE, 19, 25, poor = 20.0, average = 30.0, good = 38.0, excellent = 45.0, elite = 52.0),
            Benchmark("situps", Gender.FEMALE, 19, 25, poor = 12.0, average = 22.0, good = 30.0, excellent = 38.0, elite = 45.0),

            Benchmark("vertical_jump", Gender.MALE, 14, 18, poor = 25.0, average = 35.0, good = 43.0, excellent = 50.0, elite = 58.0),
            Benchmark("vertical_jump", Gender.FEMALE, 14, 18, poor = 18.0, average = 27.0, good = 34.0, excellent = 40.0, elite = 47.0),
            Benchmark("vertical_jump", Gender.MALE, 19, 25, poor = 30.0, average = 40.0, good = 48.0, excellent = 55.0, elite = 63.0),
            Benchmark("vertical_jump", Gender.FEMALE, 19, 25, poor = 22.0, average = 30.0, good = 37.0, excellent = 44.0, elite = 50.0),

            Benchmark("shuttle_run", Gender.MALE, 14, 18, poor = 14.0, average = 12.5, good = 11.5, excellent = 10.5, elite = 9.5),
            Benchmark("shuttle_run", Gender.FEMALE, 14, 18, poor = 15.0, average = 13.5, good = 12.5, excellent = 11.5, elite = 10.5),
            Benchmark("shuttle_run", Gender.MALE, 19, 25, poor = 13.5, average = 12.0, good = 11.0, excellent = 10.0, elite = 9.0),
            Benchmark("shuttle_run", Gender.FEMALE, 19, 25, poor = 14.5, average = 13.0, good = 12.0, excellent = 11.0, elite = 10.0),

            Benchmark("endurance_run_800m", Gender.MALE, 14, 18, poor = 220.0, average = 195.0, good = 175.0, excellent = 160.0, elite = 145.0),
            Benchmark("endurance_run_800m", Gender.FEMALE, 14, 18, poor = 250.0, average = 220.0, good = 200.0, excellent = 185.0, elite = 170.0),
            Benchmark("endurance_run_800m", Gender.MALE, 19, 25, poor = 210.0, average = 185.0, good = 165.0, excellent = 150.0, elite = 135.0),
            Benchmark("endurance_run_800m", Gender.FEMALE, 19, 25, poor = 240.0, average = 210.0, good = 190.0, excellent = 175.0, elite = 160.0),

            Benchmark("endurance_run_1600m", Gender.MALE, 14, 18, poor = 480.0, average = 420.0, good = 380.0, excellent = 340.0, elite = 300.0),
            Benchmark("endurance_run_1600m", Gender.FEMALE, 14, 18, poor = 540.0, average = 480.0, good = 430.0, excellent = 390.0, elite = 350.0),
            Benchmark("endurance_run_1600m", Gender.MALE, 19, 25, poor = 460.0, average = 400.0, good = 360.0, excellent = 320.0, elite = 280.0),
            Benchmark("endurance_run_1600m", Gender.FEMALE, 19, 25, poor = 520.0, average = 460.0, good = 410.0, excellent = 370.0, elite = 330.0)
        )
    }
}
