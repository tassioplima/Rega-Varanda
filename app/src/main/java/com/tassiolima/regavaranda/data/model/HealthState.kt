package com.tassiolima.regavaranda.data.model

enum class HealthState(val label: String, val emoji: String, val score: Int) {
    SAUDAVEL("Saudável", "💚", 2),
    ATENCAO("Atenção", "🟡", 1),
    CRITICA("Crítica", "🔴", 0)
}
