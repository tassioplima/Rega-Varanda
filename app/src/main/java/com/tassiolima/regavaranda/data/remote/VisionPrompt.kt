package com.tassiolima.regavaranda.data.remote

import com.tassiolima.regavaranda.data.model.HealthState
import com.tassiolima.regavaranda.data.model.PlantCategory
import org.json.JSONObject

/** Prompt e parsing do JSON de resposta, compartilhados entre provedores de IA (Anthropic, Gemini, ...). */
object VisionPrompt {

    /**
     * Exemplos comuns de espécie -> categoria de cuidado, para orientar a classificação
     * automática (ex.: Monstera e Hera são folhagem tropical, não "outra planta" genérica).
     * A IA ainda deve confiar na própria identificação visual para espécies fora desta lista.
     */
    private val SPECIES_GUIDE = """
        Guia de referência de espécies comuns por tipo de cuidado, incluindo nomes populares
        usados no Brasil e em Portugal/resto da Europa (use como apoio; identifique a planta pela
        aparência visual e conhecimento botânico geral, não só pelo nome — funciona para qualquer
        espécie do mundo, mesmo fora desta lista ou com nome regional diferente):
        - ${PlantCategory.FOLHAGEM_TROPICAL.name} (${PlantCategory.FOLHAGEM_TROPICAL.label}): Monstera/Costela-de-adão, Jiboia/Pothos/Hera-do-diabo, Hera/Hera-inglesa (Hedera helix), Filodendro, Samambaia/Feto, Zamioculca, Aglaonema, Calathea, Maranta, Singônio, Peperômia, Dracena/Pau-d'água, Palmeira-ráfis/Areca, Espada-de-são-jorge/Rabo-de-tigre (Sansevieria), Antúrio (folhagem), Ficus/Figueira-de-borracha, Planta-aranha/Clorofito
        - ${PlantCategory.CACTO_SUCULENTA.name} (${PlantCategory.CACTO_SUCULENTA.label}): Echeveria, Suculenta-jade/Planta-do-dinheiro/Árvore-da-felicidade (Crassula), Aloe vera/Babosa, Haworthia, Sedum, Cordão-de-pérolas, Cordão-de-rainha, Cacto-mandacaru, Cacto-de-natal (Schlumbergera), Coroa-de-cristo, Agave, Cacto-bola, Ecévéria, Sempre-viva (Sempervivum)
        - ${PlantCategory.ORQUIDEA.name} (${PlantCategory.ORQUIDEA.label}): Orquídea-borboleta (Phalaenopsis), Cattleya, Dendrobium, Oncídio, Vanda
        - ${PlantCategory.FLOR_ORNAMENTAL.name} (${PlantCategory.FLOR_ORNAMENTAL.label}): Rosa/Roseira, Begônia, Petúnia, Gerânio/Malva (Pelargonium, comum em varandas portuguesas), Hibisco, Copo-de-leite, Violeta-africana, Azaleia, Bromélia (flor), Crisântemo, Lírio, Bem-me-quer/Margarida, Lavanda, Hortênsia
        - ${PlantCategory.HERVA_TEMPERO.name} (${PlantCategory.HERVA_TEMPERO.label}): Manjericão/Basilicão, Hortelã, Alecrim, Salsa/Salsinha, Cebolinho, Orégano, Tomilho, Sálvia, Coentro, Louro
        - ${PlantCategory.HORTALICA_LEGUME.name} (${PlantCategory.HORTALICA_LEGUME.label}): Tomate-cereja, Pimentão/Pimento, Pimenta/Piripíri, Alface, Rúcula, Pepino, Morango, Abobrinha/Curgete, Berinjela/Beringela
    """.trimIndent()

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

        val categoryOptions = PlantCategory.entries.joinToString(", ") { "\"${it.name}\" (${it.label})" }

        return """
            Você é um especialista em botânica e jardinagem de varanda/apartamento, com conhecimento
            de espécies do mundo todo (não só do Brasil — também comuns em Portugal e no resto da
            Europa). Analise esta foto de uma planta que o dono chama de "$plantName" (categoria
            informada pelo dono: ${category.label}). Identifique a espécie/tipo real da planta pela
            aparência visual na foto, mesmo que o nome dado pelo dono seja um nome regional, não seja
            o nome científico, ou não conste em nenhuma lista de referência.
            $notesLine

            $historyBlock

            $SPECIES_GUIDE

            Responda EXCLUSIVAMENTE com um JSON válido (sem markdown, sem texto antes ou depois),
            exatamente neste formato:
            {
              "identified_species": "nome popular (e científico entre parênteses se souber) da espécie identificada na foto, ex: 'Suculenta Echeveria (Echeveria elegans)'",
              "suggested_category": uma destas opções, a que melhor combina com o que você vê na foto: $categoryOptions,
              "recommended_watering_interval_days": número inteiro de dias entre regas ideal para ESSA espécie específica em condições normais (ex: 1, 3, 7, 15, 30 — use o valor típico e conhecido para a espécie identificada, não um chute genérico),
              "health_state": "SAUDAVEL" ou "ATENCAO" ou "CRITICA",
              "diagnosis": "resumo curto (1-2 frases) do que você observa na foto",
              "evolution_note": "comparação com o histórico anterior (ex: 'Melhorando: as folhas amareladas da última foto já não aparecem.'), ou string vazia se não houver histórico",
              "watering_tip": "dica objetiva sobre rega para esta espécie, ou 'A rega parece adequada.' se estiver tudo bem",
              "pruning_tip": "dica objetiva sobre poda, ou 'Não é necessário podar agora.' se não precisar",
              "fertilizing_tip": "dica objetiva sobre adubação, indicando o TIPO DE PRODUTO mais adequado para esta espécie específica (ex.: 'Adubo líquido NPK balanceado diluído na água de rega, a cada 15 dias' ou 'Adubo de liberação lenta em grânulos (tipo Osmocote), a cada 3 meses' ou 'Adubo foliar rico em fósforo para estimular floração' ou 'Adubo específico para orquídeas, diluído pela metade da dose'), ou 'Não é necessário adubar agora.' se não precisar",
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

        val suggestedCategory = PlantCategory.entries.firstOrNull { it.name == result.optString("suggested_category") }

        return PhotoAnalysisResult(
            healthState = healthState,
            diagnosis = result.optString("diagnosis", ""),
            wateringTip = result.optString("watering_tip", ""),
            pruningTip = result.optString("pruning_tip", ""),
            fertilizingTip = result.optString("fertilizing_tip", ""),
            repottingTip = result.optString("repotting_tip", ""),
            identifiedSpecies = result.optString("identified_species", ""),
            recommendedWateringIntervalDays = intervalDays,
            evolutionNote = result.optString("evolution_note", ""),
            suggestedCategory = suggestedCategory
        )
    }
}
