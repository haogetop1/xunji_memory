package com.xunji.core.study

import com.xunji.core.model.AppSettings
import com.xunji.core.model.CardSchedule
import com.xunji.core.time.LocalDay
import java.time.Instant
import java.time.ZoneId

object NewCardQuota {
    fun remaining(
        allSchedules: List<CardSchedule>,
        settings: AppSettings,
        at: Instant,
        zone: ZoneId,
    ): Int {
        val day = LocalDay.bounds(at, zone)
        val used = allSchedules.count { schedule ->
            val reviewedAt = schedule.firstReviewedAt
            reviewedAt != null && reviewedAt >= day.start && reviewedAt < day.endExclusive
        }
        return (settings.newCardsPerDay - used).coerceAtLeast(0)
    }
}
