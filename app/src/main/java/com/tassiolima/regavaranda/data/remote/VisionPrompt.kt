package com.tassiolima.regavaranda.data.remote

import com.tassiolima.regavaranda.data.model.HealthState
import com.tassiolima.regavaranda.data.model.PlantCategory
import org.json.JSONObject

/** Prompt e parsing do JSON de resposta, compartilhados entre provedores de IA (Anthropic, Gemini, ...). */
object VisionPrompt {

    fun build(plantName: String, category: PlantCategory, userNotes: String, historySummary: String): String {
        val notesLine = if (userNotes.isNotBlank()) "Observações do dono sobre esta planta: $userNotes" else ""
        val historyBlock = if (historySummary.isNotBlank()) {
            """
            Histórico de análises anteriores desta mesma planta (da mais antiga para a mais recente):
            $historySummary

            Compare a foto atual com esse histórico e diga explicitamente se a planta está melhorando,
            piorando ou estável, e por quê (baseado no que você vê agora comparado ao que foi observado antes).
            """.trimIndent()
        } else {
            "Esta é a primeira foto registrada desta planta, então não há histórico para comparar."
        }

        return """
            Você é um especialista em botânica e jardinagem de varanda/apartamento.
            Analise esta foto de uma planta que o dono chama de "$plantName" (categoria informada pelo
            dono: ${category.label}). Identifique a espécie/tipo real da planta pela aparência na foto,
            mesmo que o nome dado pelo dono não seja o nome científico ou popular correto.
            $notesLine

            $historyBlock

            Responda EXCLUSIVAMENTE com um JSON válido (sem markdown, sem texto antes ou depois),
            exatamente neste formato:
            {
              "identified_species": "nome popular (e científico entre parênteses se souber) da espécie identificada na foto, ex: 'Suculenta Echeveria (Echeveria elegans)'",
              "recommended_watering_interval_days": número inteiro de dias entre regas ideal para ESSA espécie específica em condições normais (ex: 1, 3, 7, 15, 30 — use o valor típico e conhecido para a espécie identificada, não um chute genérico),
              "health_state": "SAUDAVEL" ou "ATENCAO" ou "CRITICA",
              "diagnosis": "resumo curto (1-2 frases) do que você observa na foto",
              "evolution_note": "comparação com o histórico anterior (ex: 'Melhorando: as folhas amareladas da última foto já não aparecem.'), ou string vazia se não houver histórico",
              "watering_tip": "dica objetiva sobre rega para esta espécie, ou 'A rega parece adequada.' se estiver tudo bem",
              "pruning_tip": "dica objetiva sobre poda, ou 'Não é necessário podar agora.' se não precisar",
              "fertilizing_tip": "dica objetiva sobre adubação/vitaminas, ou 'Não é necessário adubar agora.' se não precisar",
              "repotting_tip": "dica objetiva sobre troca de vaso/transplante, ou 'O vaso atual parece adequado.' se não precisar"
            }
        """.trimIndent()
    }

    fun parse(rawText: String): PhotoAnalysisResult {
        val cleaned = rawText.trim()
            .removePrefix("```json").removePrefix("```")
            .removeSuffix("```")
            .trim()

        val result = JSONObject(cleaned)
        val healthState = HealthState.entries.firstOrNull { it.name == result.optString("health_state") }
            ?: HealthState.ATENCAO

        val intervalDays = if (result.has("recommended_watering_interval_days") && !result.isNull("recommended_watering_interval_days")) {
            result.optInt("recommended_watering_interval_days", -1).takeIf { it > 0 }
        } else null

        return PhotoAnalysisResult(
            healthState = healthState,
            diagnosis = result.optString("diagnosis", ""),
            wateringTip = result.optString("watering_tip", ""),
            pruningTip = result.optString("pruning_tip", ""),
            fertilizingTip = result.optString("fertilizing_tip", ""),
            repottingTip = result.optString("repotting_tip", ""),
            identifiedSpecies = result.optString("identified_species", ""),
            recommendedWateringIntervalDays = intervalDays,
            evolutionNote = result.optString("evolution_note", "")
        )
    }
}
