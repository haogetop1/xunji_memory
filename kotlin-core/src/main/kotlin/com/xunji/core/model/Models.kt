package com.xunji.core.model

enum class Phase { NEW, LEARNING, REVIEW, RELEARNING }

enum class ThemeMode { SYSTEM, LIGHT, DARK }

data class AppSettings(
    val newCardsPerDay: Int = 20,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val schemaVersion: Int = 1,
)

data class Flashcard(
    val id: String,
    val deckId: String,
    val front: String,
    val back: String,
    val example: String,
    val createdAt: Long,
    val updatedAt: Long,
)

data class CardSchedule(
    val cardId: String,
    val phase: Phase,
    val step: Int?,
    val stability: Double?,
    val difficulty: Double?,
    val dueAt: Long,
    val lastReviewAt: Long?,
    val firstReviewedAt: Long?,
    val revision: Long,
)

data class CardCandidate(
    val card: Flashcard,
    val schedule: CardSchedule,
    val deckName: String,
)

enum class Rating { AGAIN, HARD, GOOD, EASY }

data class ReviewLog(
    val id: String,
    val cardId: String,
    val deckId: String,
    val sessionId: String,
    val rating: Rating,
    val reviewedAt: Long,
    val durationMs: Long,
    val beforeSchedule: CardSchedule,
    val afterSchedule: CardSchedule,
    val undoneAt: Long?,
)
