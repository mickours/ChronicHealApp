package org.chronicheal.app.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class AiMealAnalysis(
    val name: String? = null,
    val ingredients: List<AiIngredient>? = null,
    val allergens: List<String>? = null,
    val fodmaps: List<String>? = null,
    val proteins: Double? = null,
    val carbohydrates: Double? = null,
    val lipids: Double? = null,
    val note: String? = null
)

@Serializable
data class AiIngredient(
    val name: String,
    val quantity: Double? = null,
    val unit: String? = null
)
