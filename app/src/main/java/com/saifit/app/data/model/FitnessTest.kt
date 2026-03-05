package com.saifit.app.data.model

data class FitnessTest(
    val id: String,
    val name: String,
    val description: String,
    val instructions: List<String>,
    val unit: String,          
    val category: TestCategory,
    val iconName: String = "fitness_center" 
)

enum class TestCategory {
    ANTHROPOMETRIC,   
    STRENGTH,         
    POWER,            
    AGILITY,          
    ENDURANCE         
}
