package com.tassiolima.regavaranda.data.remote

import com.tassiolima.regavaranda.data.model.HealthState
import com.tassiolima.regavaranda.data.model.PlantCategory

data class PhotoAnalysisResult(
    val healthState: HealthState,
    val diagnosis: String,
    val wateringTip: String,
    val pruningTip: String,
    val fertilizingTip: String,
    val repottingTip: String,
    /** Espécie/tipo de planta identificado pela IA a partir da foto (ex.: "Suculenta Echeveria"). */
    val identifiedSpecies: String,
    /** Intervalo de rega ideal em dias para essa espécie específica, segundo a IA (ex.: 1, 15, 30). */
    val recommendedWateringIntervalDays: Int?,
    /** Comparação com fotos anteriores desta planta (vazio se esta for a primeira foto). */
    val evolutionNote: String,
    /** Categoria de cuidado que a IA reconheceu pela aparência (orquídea, suculenta, tropical...). */
    val suggestedCategory: PlantCategory?
)
