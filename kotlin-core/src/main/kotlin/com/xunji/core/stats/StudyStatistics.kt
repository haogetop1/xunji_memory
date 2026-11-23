package com.xunji.core.stats

import com.xunji.core.model.Rating
import com.xunji.core.model.ReviewLog
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

data class DaySummary(
    val date: LocalDate,
    val scoreCount: Int,
    val distinctCards: Int,
    val againRatio: Double?,
    val durationMs: Long,
)

data class StatisticsSummary(val today: DaySummary, val lastSevenDays: List<DaySummary>)

object StudyStatistics {
    fun summarize(logs: List<ReviewLog>, at: Instant, zone: ZoneId): StatisticsSummary {
        val today = at.atZone(zone).toLocalDate()
        val days = (6 downTo 0).map { offset ->
            val date = today.minusDays(offset.toLong())
            val start = date.atStartOfDay(zone).toInstant().toEpochMilli()
            val end = date.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
            val valid = logs.filter { it.undoneAt == null && it.reviewedAt >= start && it.reviewedAt < end }
            val againRatio = if (valid.isEmpty()) null else valid.count { it.rating == Rating.AGAIN }.toDouble() / valid.size
            DaySummary(
                date = date,
                scoreCount = valid.size,
                distinctCards = valid.map { it.cardId }.toSet().size,
                againRatio = againRatio,
                durationMs = valid.sumOf { it.durationMs.coerceIn(0L, 300_000L) },
            )
        }
        return StatisticsSummary(days.last(), days)
    }
}
