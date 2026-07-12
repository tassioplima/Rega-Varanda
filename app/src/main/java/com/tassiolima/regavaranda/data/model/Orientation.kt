package com.tassiolima.regavaranda.data.model

/**
 * sunFactor = fração aproximada da luz do dia em que uma fachada voltada para essa
 * direção recebe sol direto, considerando o hemisfério norte (ex.: Portugal).
 * Norte recebe o mínimo de sol direto; Sul recebe o máximo.
 */
enum class Orientation(val label: String, val sunFactor: Double) {
    NORTE("Norte", 0.15),
    NORDESTE("Nordeste", 0.35),
    LESTE("Leste (sol da manhã)", 0.55),
    SUDESTE("Sudeste", 0.75),
    SUL("Sul (sol pleno)", 0.95),
    SUDOESTE("Sudoeste", 0.85),
    OESTE("Oeste (sol da tarde)", 0.60),
    NOROESTE("Noroeste", 0.30);

    /** Centro da direção em graus, no sentido da bússola (Norte = 0°). */
    val centerDegrees: Int get() = ordinal * 45

    companion object {
        fun nearestTo(degrees: Float): Orientation {
            val normalized = ((degrees % 360) + 360) % 360
            return entries.minBy { orientation ->
                val diff = kotlin.math.abs(normalized - orientation.centerDegrees)
                minOf(diff, 360f - diff)
            }
        }
    }
}
