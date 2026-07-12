package com.tassiolima.regavaranda.domain

import com.tassiolima.regavaranda.data.local.PlantEntity
import com.tassiolima.regavaranda.data.local.WateringLogEntity
import com.tassiolima.regavaranda.data.model.SunNeed
import com.tassiolima.regavaranda.data.remote.WeatherSnapshot
import com.tassiolima.regavaranda.util.DateUtils
import kotlin.math.roundToInt

data class WateringPlan(
    val timesPerDay: Int,
    val intervalDays: Int,
    val reason: String,
    val baseIntervalSource: BaseIntervalSource
)

enum class BaseIntervalSource { MANUAL, AI_IDENTIFIED, CATEGORY_DEFAULT }

data class WateringStatus(
    val isDueNow: Boolean,
    val timesRemainingToday: Int,
    val daysUntilNext: Int,
    /** Timestamp exato da próxima rega prevista, para exibir um countdown preciso. Nulo quando já está vencida. */
    val nextDueAtMillis: Long? = null
)

object WateringScheduler {

    /** Prioridade do intervalo-base: ajuste manual do usuário > espécie identificada pela IA > padrão da categoria. */
    private fun resolveBaseInterval(plant: PlantEntity): Pair<Int, BaseIntervalSource> = when {
        plant.customIntervalDays != null -> plant.customIntervalDays to BaseIntervalSource.MANUAL
        plant.aiWateringIntervalDays != null -> plant.aiWateringIntervalDays to BaseIntervalSource.AI_IDENTIFIED
        else -> plant.category.baseWateringDays to BaseIntervalSource.CATEGORY_DEFAULT
    }

    fun computePlan(
        plant: PlantEntity,
        weather: WeatherSnapshot,
        estimatedSunHours: Double
    ): WateringPlan {
        val category = plant.category
        val (baseDays, source) = resolveBaseInterval(plant)
        var interval = baseDays.toDouble()

        val hotAndSunny = weather.maxTempC >= 30.0 && estimatedSunHours >= 6.0 && category.sunNeed != SunNeed.SOMBRA
        val warmAndSunny = weather.maxTempC >= 25.0 && estimatedSunHours >= 4.0

        when {
            hotAndSunny -> interval *= 0.4
            warmAndSunny -> interval *= 0.65
        }

        if (weather.precipitationProbabilityMax >= 60) {
            interval *= 1.6
        }

        interval = interval.coerceAtLeast(0.5)

        val sourceLabel = when (source) {
            BaseIntervalSource.MANUAL -> ""
            BaseIntervalSource.AI_IDENTIFIED -> " (espécie identificada pela IA)"
            BaseIntervalSource.CATEGORY_DEFAULT -> ""
        }

        return if (interval < 1.0) {
            val timesPerDay = (1.0 / interval).roundToInt().coerceIn(2, 3)
            val reason = "Calor forte (${weather.maxTempC.roundToInt()}°C) com bastante sol na varanda " +
                "(~${estimatedSunHours.roundToInt()}h) — regue $timesPerDay vezes hoje, de manhã e à tarde."
            WateringPlan(timesPerDay = timesPerDay, intervalDays = 1, reason = reason, baseIntervalSource = source)
        } else {
            val days = interval.roundToInt().coerceAtLeast(1)
            val reason = when {
                weather.precipitationProbabilityMax >= 60 ->
                    "Boa chance de chuva hoje (${weather.precipitationProbabilityMax}%) — intervalo esticado para $days dia(s)$sourceLabel."
                warmAndSunny ->
                    "Dia quente e com sol — regue a cada $days dia(s)$sourceLabel enquanto durar esse tempo."
                else ->
                    "Regue a cada $days dia(s)$sourceLabel."
            }
            WateringPlan(timesPerDay = 1, intervalDays = days, reason = reason, baseIntervalSource = source)
        }
    }

    fun computeStatus(plan: WateringPlan, plant: PlantEntity, nowMillis: Long): WateringStatus {
        val todayEpochDay = DateUtils.todayEpochDay()
        val lastWateredAt = plant.lastWateredAt

        if (lastWateredAt == null) {
            return WateringStatus(isDueNow = true, timesRemainingToday = plan.timesPerDay, daysUntilNext = 0)
        }

        if (plan.timesPerDay >= 2) {
            val doneToday = if (plant.waterCountDayEpoch == todayEpochDay) plant.waterCountToday else 0
            val remaining = (plan.timesPerDay - doneToday).coerceAtLeast(0)
            return WateringStatus(isDueNow = remaining > 0, timesRemainingToday = remaining, daysUntilNext = 0)
        }

        val nextDueAtMillis = lastWateredAt + plan.intervalDays * MILLIS_PER_DAY
        val lastWateredDay = DateUtils.epochDayOf(lastWateredAt)
        val daysSince = (todayEpochDay - lastWateredDay).toInt()
        val isDue = nowMillis >= nextDueAtMillis
        val daysUntilNext = (plan.intervalDays - daysSince).coerceAtLeast(0)
        return WateringStatus(
            isDueNow = isDue,
            timesRemainingToday = if (isDue) 1 else 0,
            daysUntilNext = daysUntilNext,
            nextDueAtMillis = if (isDue) null else nextDueAtMillis
        )
    }

    /** Formata o tempo restante até a próxima rega em algo como "2 dias e 5h" ou "8h" ou "42min". */
    fun formatCountdown(remainingMillis: Long): String {
        if (remainingMillis <= 0) return "agora"
        val totalMinutes = remainingMillis / 60_000L
        val days = totalMinutes / (24 * 60)
        val hours = (totalMinutes % (24 * 60)) / 60
        val minutes = totalMinutes % 60

        return when {
            days > 0 && hours > 0 -> "${days}d ${hours}h"
            days > 0 -> "${days}d"
            hours > 0 -> "${hours}h"
            else -> "${minutes}min"
        }
    }

    /**
     * Compara a frequência real de regas dos últimos 7 dias com o intervalo recomendado
     * pelo plano. Se o usuário estiver regando muito mais do que o indicado, devolve um
     * aviso; caso contrário, null. Ignorado quando o plano já pede múltiplas regas/dia
     * (calor extremo), já que nesse caso a alta frequência é esperada.
     */
    fun detectOverwatering(plan: WateringPlan, recentLog: List<WateringLogEntity>, nowMillis: Long): String? {
        if (plan.timesPerDay >= 2) return null

        val windowDays = 7.0
        val sinceMillis = nowMillis - (windowDays * MILLIS_PER_DAY).toLong()
        val recentCount = recentLog.count { it.wateredAt >= sinceMillis }
        if (recentCount < 3) return null

        val expectedCount = (windowDays / plan.intervalDays).coerceAtLeast(1.0)
        return if (recentCount >= expectedCount * 2.0) {
            "💧⚠️ Você regou esta planta $recentCount vezes nos últimos 7 dias — bem mais frequente que o " +
                "recomendado (a cada ${plan.intervalDays} dia(s)). Regar demais pode causar apodrecimento das raízes."
        } else null
    }

    /**
     * Interpreta a última leitura do medidor de umidade do solo (escala 1-10) para esta
     * planta, comparando com o limiar ideal da categoria dela. Null se nunca houve leitura.
     */
    fun moistureFeedback(plant: PlantEntity, nowMillis: Long): MoistureFeedback? {
        val level = plant.soilMoistureLevel ?: return null
        val readingAt = plant.soilMoistureReadingAt ?: return null
        val threshold = plant.category.dryMoistureThreshold

        val ageDays = ((nowMillis - readingAt) / MILLIS_PER_DAY).toInt()
        val ageLabel = when {
            ageDays <= 0 -> "medido hoje"
            ageDays == 1 -> "medido ontem"
            else -> "medido há $ageDays dias"
        }

        val message = when {
            level <= threshold ->
                "🌵 Medidor indica solo seco (nível $level/10, $ageLabel) — pode regar agora."
            level <= threshold + 2 ->
                "🌱 Medidor indica umidade média (nível $level/10, $ageLabel) — ok por enquanto."
            else ->
                "💧 Medidor indica solo ainda úmido (nível $level/10, $ageLabel) — não precisa regar ainda."
        }

        return MoistureFeedback(level = level, message = message, suggestsWatering = level <= threshold)
    }

    private const val MILLIS_PER_DAY = 86_400_000L
}

data class MoistureFeedback(val level: Int, val message: String, val suggestsWatering: Boolean)
