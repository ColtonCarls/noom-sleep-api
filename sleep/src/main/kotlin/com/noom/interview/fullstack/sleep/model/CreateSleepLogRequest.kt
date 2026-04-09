package com.noom.interview.fullstack.sleep.model

import java.time.Instant

data class CreateSleepLogRequest(
    val bedTime: Instant,
    val wakeTime: Instant,
    val feeling: MorningFeeling
)
