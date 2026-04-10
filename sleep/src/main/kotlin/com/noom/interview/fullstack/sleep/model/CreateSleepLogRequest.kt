package com.noom.interview.fullstack.sleep.model

import java.time.Instant

/**
 * Request body for creating a sleep log entry.
 * The userId is passed via the URL path, not in the body.
 * Timestamps should be provided in ISO-8601 format (e.g. "2025-11-12T22:53:00Z").
 */
data class CreateSleepLogRequest(
    val bedTime: Instant,
    val wakeTime: Instant,
    val feeling: MorningFeeling
)
