package com.xunji.core.study

class SessionTimer {
    private var accumulated = 0L
    private var started: Long? = null

    fun resume(monotonicMs: Long) {
        if (started == null) started = monotonicMs
    }

    fun pause(monotonicMs: Long) {
        accumulated = elapsedMs(monotonicMs)
        started = null
    }

    fun elapsedMs(monotonicMs: Long): Long {
        val active = started?.let { (monotonicMs - it).coerceAtLeast(0L) } ?: 0L
        return (accumulated + active).coerceIn(0L, 300_000L)
    }

    fun reset() {
        accumulated = 0L
        started = null
    }
}
