package com.noom.interview.fullstack.sleep.model

import java.time.Instant

/**
 * Request body for creating a new sleep log entry.
 * The `userId` is provided via the URL path, not in the body.
 *
 * @property bedTime when the user went to bed, in ISO-8601 format (e.g. `2026-04-08T22:53:00Z`).
 * @property wakeTime when the user woke up; must be after [bedTime].
 * @property feeling how the user felt in the morning.
 */
data class CreateSleepLogRequest(
    val bedTime: Instant,
    val wakeTime: Instant,
    val feeling: MorningFeeling
)
