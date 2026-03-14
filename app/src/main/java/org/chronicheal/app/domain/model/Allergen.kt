package org.chronicheal.app.domain.model

import org.chronicheal.app.R

enum class Allergen(val id: String, val displayRes: Int) {
    GLUTEN("gluten", R.string.allergen_gluten),
    LACTOSE("lactose", R.string.allergen_lactose),
    NUTS("nuts", R.string.allergen_nuts),
    SHELLFISH("shellfish", R.string.allergen_shellfish),
    EGGS("eggs", R.string.allergen_eggs),
    FISH("fish", R.string.allergen_fish),
    SOY("soy", R.string.allergen_soy),
    WHEAT("wheat", R.string.allergen_wheat);

    companion object {
        fun fromId(id: String): Allergen? = entries.find { it.id == id }
        
        val allIds = entries.map { it.id }
    }
}
