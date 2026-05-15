package com.example.janaushadifinder.domain.model

data class MedicineSearch(
    val brandedName: String,
    val saltName: String,
    val brandedPrice: Double,
    val genericPrice: Double,
    val category: String = "General",
    val savingsPercentage: Int = (((brandedPrice - genericPrice) / brandedPrice) * 100).toInt()
)
