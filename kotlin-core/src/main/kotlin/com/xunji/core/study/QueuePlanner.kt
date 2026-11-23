package com.xunji.core.study

import com.xunji.core.model.CardCandidate
import com.xunji.core.model.Phase
import java.time.Instant

sealed interface QueueDecision {
    data class Ready(val candidate: CardCandidate) : QueueDecision
    data class Waiting(val nextDueAt: Long) : QueueDecision
    data object Done : QueueDecision
}

object QueuePlanner {
    private val dueOrder = compareBy<CardCandidate> { if (it.schedule.phase == Phase.REVIEW) 1 else 0 }
        .thenBy { it.schedule.dueAt }
        .thenBy { it.card.id }

    private val newOrder = compareBy<CardCandidate> { it.card.createdAt }
        .thenBy { it.card.id }

    fun next(candidates: List<CardCandidate>, remainingNew: Int, at: Instant): QueueDecision {
        val due = candidates
            .filter { it.schedule.phase != Phase.NEW && it.schedule.dueAt <= at.toEpochMilli() }
            .minWithOrNull(dueOrder)
        if (due != null) return QueueDecision.Ready(due)

        if (remainingNew > 0) {
            val nextNew = candidates
                .filter { it.schedule.phase == Phase.NEW }
                .minWithOrNull(newOrder)
            if (nextNew != null) return QueueDecision.Ready(nextNew)
        }

        val nextDueAt = candidates
            .filter { it.schedule.phase != Phase.NEW }
            .minOfOrNull { it.schedule.dueAt }
        return nextDueAt?.let(QueueDecision::Waiting) ?: QueueDecision.Done
    }
}
