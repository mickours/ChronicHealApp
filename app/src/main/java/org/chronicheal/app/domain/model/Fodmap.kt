package org.chronicheal.app.domain.model

import org.chronicheal.app.R

enum class Fodmap(val id: String, val displayRes: Int) {
    FRUCTANS("fructans", R.string.fodmap_fructans),
    GOS("gos", R.string.fodmap_gos),
    FRUCTOSE("fructose", R.string.fodmap_fructose),
    SORBITOL("sorbitol", R.string.fodmap_sorbitol),
    MANNITOL("mannitol", R.string.fodmap_mannitol),
    LACTOSE("lactose_fodmap", R.string.fodmap_lactose);

    companion object {
        fun fromId(id: String): Fodmap? = entries.find { it.id == id }

        val allIds = entries.map { it.id }
    }
}
