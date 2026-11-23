package com.xunji.core

import com.xunji.core.model.AppSettings
import com.xunji.core.model.CardCandidate
import com.xunji.core.model.CardSchedule
import com.xunji.core.model.Flashcard
import com.xunji.core.model.Phase
import com.xunji.core.model.Rating
import com.xunji.core.model.ReviewLog
import com.xunji.core.stats.StudyStatistics
import com.xunji.core.study.NewCardQuota
import com.xunji.core.study.QueueDecision
import com.xunji.core.study.QueuePlanner
import com.xunji.core.study.SessionTimer
import com.xunji.core.time.LocalDay
import com.xunji.core.validation.FieldValidationException
import com.xunji.core.validation.InputValidator
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset

fun main() {
    checkQueue()
    checkQuota()
    checkTimer()
    checkValidation()
    checkStatistics()
    println("Passed five behavior checks.")
}

private fun card(id: String, phase: Phase, dueAt: Long, createdAt: Long = 0) = CardCandidate(
    Flashcard(id, "deck", "front", "back", "", createdAt, createdAt),
    CardSchedule(id, phase, null, null, null, dueAt, null, null, 0),
    "Deck",
)

private fun checkQueue() {
    val now = Instant.ofEpochMilli(100)
    val review = card("earlier-review", Phase.REVIEW, 0)
    val learningA = card("a", Phase.LEARNING, 100)
    val learningB = card("b", Phase.RELEARNING, 100)
    val newLate = card("a-new", Phase.NEW, 2_000, 20)
    val newEarly = card("b-new", Phase.NEW, 2_000, 10)
    val future = card("future", Phase.REVIEW, 101)
    val evenLater = card("later", Phase.LEARNING, 200)
    check(QueuePlanner.next(listOf(review, learningB, learningA, newEarly), 0, now)
        == QueueDecision.Ready(learningA))
    check(QueuePlanner.next(listOf(review, newEarly), 2, now) == QueueDecision.Ready(review))
    check(QueuePlanner.next(listOf(future, newLate, newEarly), 1, now) == QueueDecision.Ready(newEarly))
    check(QueuePlanner.next(listOf(evenLater, newEarly, future), 0, now) == QueueDecision.Waiting(101))
    check(QueuePlanner.next(listOf(newEarly), -1, now) == QueueDecision.Done)
    check(QueuePlanner.next(emptyList(), 0, now) == QueueDecision.Done)
}

private fun checkQuota() {
    val now = Instant.parse("2025-03-09T18:00:00Z")
    val newYork = ZoneId.of("America/New_York")
    val day = LocalDay.bounds(now, newYork)
    check(day.start == Instant.parse("2025-03-09T05:00:00Z").toEpochMilli())
    check(day.endExclusive == Instant.parse("2025-03-10T04:00:00Z").toEpochMilli())
    check(LocalDay.bounds(now, ZoneOffset.UTC).endExclusive -
        LocalDay.bounds(now, ZoneOffset.UTC).start == 86_400_000L)

    val schedule = card("a", Phase.REVIEW, 0).schedule
    val schedules = listOf(null, day.start - 1, day.start, day.endExclusive - 1, day.endExclusive)
        .map { schedule.copy(firstReviewedAt = it) }
    check(NewCardQuota.remaining(schedules, AppSettings(4), now, newYork) == 2)
    check(NewCardQuota.remaining(schedules, AppSettings(1), now, newYork) == 0)
    check(NewCardQuota.remaining(emptyList(), AppSettings(), now, newYork) == 20)
}

private fun checkTimer() {
    val timer = SessionTimer()
    check(timer.elapsedMs(100) == 0L)
    timer.resume(100)
    timer.resume(200)
    check(timer.elapsedMs(90) == 0L)
    check(timer.elapsedMs(300) == 200L)
    timer.pause(400)
    check(timer.elapsedMs(700) == 300L)
    timer.resume(1_000)
    check(timer.elapsedMs(1_010) == 310L)
    check(timer.elapsedMs(900_000) == 300_000L)
    timer.reset()
    check(timer.elapsedMs(900_000) == 0L)
}

private fun checkValidation() {
    check(InputValidator.deckName("  循记  ") == "循记")
    check(InputValidator.deckName("🀄".repeat(80)).codePointCount(0, 160) == 80)
    InputValidator.cardContent("🀄".repeat(2_000), "a".repeat(10_000), "a".repeat(4_000))
    InputValidator.cardContent("正面", "反面", " ")
    checkError("name", "请填写此项") { InputValidator.deckName(" \t") }
    checkError("front", "最多填写 2000 个字符") {
        InputValidator.cardContent("🀄".repeat(2_001), "反面", "")
    }
    checkError("back", "请填写此项") { InputValidator.cardContent("正面", "", "") }
    checkError("example", "最多填写 4000 个字符") {
        InputValidator.cardContent("正面", "反面", "a".repeat(4_001))
    }
}

private fun checkError(field: String, message: String, action: () -> Unit) {
    val error = runCatching(action).exceptionOrNull()
    check(error is FieldValidationException && error.field == field && error.message == message)
}

private fun checkStatistics() {
    val now = Instant.parse("2025-03-09T18:00:00Z")
    val zone = ZoneId.of("America/New_York")
    val day = LocalDay.bounds(now, zone)
    val schedule = card("a", Phase.REVIEW, 0).schedule
    val first = ReviewLog(
        "first", "a", "deck", "session", Rating.AGAIN, day.start, 12, schedule, schedule, null,
    )
    val logs = listOf(
        first,
        first.copy(id = "second", rating = Rating.GOOD, reviewedAt = day.endExclusive - 1, durationMs = 500_000),
        first.copy(id = "undone", cardId = "b", reviewedAt = day.start + 1, undoneAt = day.start + 2),
        first.copy(id = "yesterday", reviewedAt = day.start - 1, durationMs = -100),
        first.copy(id = "tomorrow", reviewedAt = day.endExclusive),
    )
    val summary = StudyStatistics.summarize(logs, now, zone)
    check(summary.lastSevenDays.size == 7)
    check(summary.lastSevenDays.first().date == summary.today.date.minusDays(6))
    check(summary.lastSevenDays.first().againRatio == null)
    check(summary.lastSevenDays[5].scoreCount == 1 && summary.lastSevenDays[5].durationMs == 0L)
    check(summary.today == summary.lastSevenDays.last())
    check(summary.today.scoreCount == 2 && summary.today.distinctCards == 1)
    check(summary.today.againRatio == 0.5 && summary.today.durationMs == 300_012L)
}
