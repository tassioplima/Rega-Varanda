package com.tassiolima.regavaranda.data.model

enum class SunNeed {
    SOL_PLENO,
    MEIA_SOMBRA,
    SOMBRA
}

enum class PlantCategory(
    val label: String,
    val emoji: String,
    val baseWateringDays: Int,
    val fertilizingDays: Int,
    val sunNeed: SunNeed,
    val careTip: String,
    /** Nível no medidor de umidade (escala 1-10, 1=seco/10=molhado) em que já é hora de regar. */
    val dryMoistureThreshold: Int
) {
    CACTO_SUCULENTA(
        label = "Cacto / Suculenta",
        emoji = "🪴",
        baseWateringDays = 12,
        fertilizingDays = 60,
        sunNeed = SunNeed.SOL_PLENO,
        careTip = "Deixe a terra secar totalmente entre regas. Excesso de água é a causa mais comum de morte dessas plantas.",
        dryMoistureThreshold = 2
    ),
    HORTALICA_LEGUME(
        label = "Hortaliça / Legume",
        emoji = "🍅",
        baseWateringDays = 1,
        fertilizingDays = 14,
        sunNeed = SunNeed.SOL_PLENO,
        careTip = "Precisa de solo sempre úmido (não encharcado) e pelo menos 6h de sol direto para frutificar bem.",
        dryMoistureThreshold = 5
    ),
    HERVA_TEMPERO(
        label = "Ervas / Temperos",
        emoji = "🌿",
        baseWateringDays = 2,
        fertilizingDays = 21,
        sunNeed = SunNeed.MEIA_SOMBRA,
        careTip = "Regue quando os primeiros centímetros da terra secarem. Colha regularmente para estimular novos brotos.",
        dryMoistureThreshold = 4
    ),
    FLOR_ORNAMENTAL(
        label = "Flor ornamental",
        emoji = "🌺",
        baseWateringDays = 3,
        fertilizingDays = 14,
        sunNeed = SunNeed.MEIA_SOMBRA,
        careTip = "Remova flores murchas para estimular novas floradas e evite molhar as flores diretamente.",
        dryMoistureThreshold = 4
    ),
    FOLHAGEM_TROPICAL(
        label = "Folhagem tropical",
        emoji = "🌿",
        baseWateringDays = 4,
        fertilizingDays = 30,
        sunNeed = SunNeed.SOMBRA,
        careTip = "Prefere luz indireta e ambiente úmido. Borrife água nas folhas em dias muito secos.",
        dryMoistureThreshold = 5
    ),
    ORQUIDEA(
        label = "Orquídea",
        emoji = "🌸",
        baseWateringDays = 7,
        fertilizingDays = 30,
        sunNeed = SunNeed.MEIA_SOMBRA,
        careTip = "Regue por imersão do vaso em água à temperatura ambiente por alguns minutos (ou com o método do cubo de gelo), evitando molhar a coroa. Prefira substrato de casca/fibra em vez de terra comum, com luz indireta forte.",
        dryMoistureThreshold = 3
    ),
    OUTRA(
        label = "Outra planta",
        emoji = "🌱",
        baseWateringDays = 3,
        fertilizingDays = 21,
        sunNeed = SunNeed.MEIA_SOMBRA,
        careTip = "Ajuste a frequência de rega conforme a necessidade observada da planta.",
        dryMoistureThreshold = 4
    )
}
